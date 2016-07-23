ALTER TABLE collection DROP CONSTRAINT IF EXISTS collection_card_id_quality_foil_list_id_key;
ALTER TABLE collection ADD CONSTRAINT collection_card_id_quality_foil_key UNIQUE (card_id, quality, foil);

ALTER TABLE collection DROP COLUMN IF EXISTS list_id;

DROP TRIGGER IF EXISTS collection_enforce_list_quality_rules_trigger ON collection;
DROP FUNCTION IF EXISTS enforce_collection_row_list_quality_rules();

ALTER TABLE collection DROP CONSTRAINT collection_quality_check;
ALTER TABLE collection ADD CONSTRAINT collection_quality_check CHECK (quality IN ('online', 'near mint', 'lightly played', 'moderately played', 'heavily played', 'damaged'));
ALTER TABLE collection ALTER COLUMN quality SET NOT NULL;

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
			WHERE cl.card_id = update_card_id
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
			WHERE cl.card_id = update_card_id AND cl.foil = TRUE
		)
	WHERE id = update_card_id;
	RETURN NULL;
END;
$update_card_owned_foil_count$ LANGUAGE plpgsql;
