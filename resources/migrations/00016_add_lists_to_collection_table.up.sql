ALTER TABLE collection ADD COLUMN list_id INT;

ALTER TABLE collection DROP CONSTRAINT IF EXISTS collection_card_id_quality_foil_key;
ALTER TABLE collection ADD CONSTRAINT collection_card_id_quality_foil_list_id_key UNIQUE (card_id, quality, foil, list_id);

-- all existing cards in the collections table at this point will have been added because
-- they are owned, so put them in the default 'Owned' list
UPDATE collection SET list_id = 0;

ALTER TABLE collection ALTER COLUMN list_id SET NOT NULL;
ALTER TABLE ONLY collection ADD CONSTRAINT collection_list_id_fkey FOREIGN KEY (list_id) REFERENCES lists (id) ON DELETE CASCADE;
CREATE INDEX collection_list_id_idx ON collection (list_id);


-- restriction that checks whether the corresponding list has 'require_qualities = TRUE' and then
-- throws an error if the new/updated collection row has a null quality. also enforces the reverse,
-- if 'require_qualities = FALSE' throws an error if a non-null quality is used

CREATE OR REPLACE FUNCTION enforce_collection_row_list_quality_rules()
	RETURNS TRIGGER
AS $enforce_collection_row_list_quality_rules$
DECLARE
	require_qualities BOOLEAN;
BEGIN
	SELECT lists.require_qualities INTO STRICT require_qualities
	FROM lists WHERE lists.id = NEW.list_id;

	IF (NEW.quality IS NULL AND require_qualities = TRUE) THEN
		RAISE EXCEPTION 'NULL quality invalid when list.require_qualities = TRUE';
	ELSIF (NEW.quality IS NOT NULL AND require_qualities = FALSE) THEN
		RAISE EXCEPTION 'non-NULL quality invalid when list.require_qualities = FALSE';
	END IF;

	RETURN NEW;
END;
$enforce_collection_row_list_quality_rules$ LANGUAGE plpgsql;

CREATE TRIGGER collection_enforce_list_quality_rules_trigger
BEFORE INSERT OR UPDATE ON collection
FOR EACH ROW EXECUTE PROCEDURE enforce_collection_row_list_quality_rules();

ALTER TABLE collection DROP CONSTRAINT collection_quality_check;
ALTER TABLE collection ADD CONSTRAINT collection_quality_check CHECK (quality IN (NULL, 'online', 'near mint', 'lightly played', 'moderately played', 'heavily played', 'damaged'));
ALTER TABLE collection ALTER COLUMN quality DROP NOT NULL;


-- also need to update the trigger functions which set the cards table owned count fields
-- to grab the card inventory count from the new 'Owned' list (list_id = 0)

CREATE OR REPLACE FUNCTION update_card_owned_count()
	RETURNS TRIGGER
AS $update_card_owned_count$
DECLARE
	update_card_id TEXT;
BEGIN
	IF (TG_OP = 'DELETE') THEN
		update_card_id = OLD.card_id;
	ELSE
		update_card_id = NEW.card_id;
	END IF;

	UPDATE cards
	SET
		owned_count = (
			SELECT COALESCE(SUM(quantity), 0)
			FROM collection cl
			WHERE cl.card_id = update_card_id AND cl.list_id = 0
		)
	WHERE id = update_card_id;
	RETURN NULL;
END;
$update_card_owned_count$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_card_owned_foil_count()
	RETURNS TRIGGER
AS $update_card_owned_foil_count$
DECLARE
	update_card_id TEXT;
BEGIN
	IF (TG_OP = 'DELETE') THEN
		update_card_id = OLD.card_id;
	ELSE
		update_card_id = NEW.card_id;
	END IF;

	UPDATE cards
	SET
		owned_foil_count = (
			SELECT COALESCE(SUM(quantity), 0)
			FROM collection cl
			WHERE cl.card_id = update_card_id AND cl.foil = TRUE AND cl.list_id = 0
		)
	WHERE id = update_card_id;
	RETURN NULL;
END;
$update_card_owned_foil_count$ LANGUAGE plpgsql;