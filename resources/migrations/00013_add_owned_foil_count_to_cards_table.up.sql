ALTER TABLE cards ADD COLUMN owned_foil_count INT NOT NULL DEFAULT 0;

CREATE INDEX cards_owned_foil_count_idx ON cards (owned_foil_count);

--
-- trigger to run whenever a collection row is added/updated that fills in
-- the owned_foil_count column in the corresponding card
--

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

CREATE TRIGGER cards_update_owned_foil_count_trigger
AFTER INSERT OR UPDATE OR DELETE ON collection
FOR EACH ROW EXECUTE PROCEDURE update_card_owned_foil_count();


--
-- fill in owned_foil_count in any existing cards
--

UPDATE cards c
SET
	owned_foil_count = (
		SELECT COALESCE(SUM(quantity), 0)
		FROM collection cl
		WHERE cl.card_id = c.id AND cl.foil = TRUE
	);
