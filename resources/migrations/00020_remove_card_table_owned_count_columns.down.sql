-- ugh, this migration really makes me question whether "down" migrations are actually worthwhile...
-- i honestly can't think of any time where i've wanted to run a "down" migration beyond the initial
-- dev/testing of the "up" migration.



---------------------------------------------------------------------------------------
-- copy of 00009_add_owned_count_to_cards_table.up.sql

ALTER TABLE cards ADD COLUMN owned_count INT NOT NULL DEFAULT 0;

CREATE INDEX cards_owned_count_idx ON cards (owned_count);

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

CREATE TRIGGER cards_update_owned_count_trigger
AFTER INSERT OR UPDATE OR DELETE ON collection
FOR EACH ROW EXECUTE PROCEDURE update_card_owned_count();

UPDATE cards c
SET
	owned_count = (
		SELECT COALESCE(SUM(quantity), 0)
		FROM collection cl
		WHERE cl.card_id = c.id AND cl.list_id = 0
	);




---------------------------------------------------------------------------------------
-- copy of 00013_add_owned_foil_count_to_cards_table.up.sql

ALTER TABLE cards ADD COLUMN owned_foil_count INT NOT NULL DEFAULT 0;

CREATE INDEX cards_owned_foil_count_idx ON cards (owned_foil_count);

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

CREATE TRIGGER cards_update_owned_foil_count_trigger
AFTER INSERT OR UPDATE OR DELETE ON collection
FOR EACH ROW EXECUTE PROCEDURE update_card_owned_foil_count();

UPDATE cards c
SET
	owned_foil_count = (
		SELECT COALESCE(SUM(quantity), 0)
		FROM collection cl
		WHERE cl.card_id = c.id AND cl.foil = TRUE AND cl.list_id = 0
	);


---------------------------------------------------------------------------------------
-- copy (part of) 00014_add_better_cards_table_sort_indexes.up.sql

CREATE INDEX cards_owned_count_asc_id_idx ON cards (owned_count ASC, id);
CREATE INDEX cards_owned_count_desc_id_idx ON cards (owned_count DESC, id);
