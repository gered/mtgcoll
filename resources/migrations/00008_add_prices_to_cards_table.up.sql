ALTER TABLE cards ADD COLUMN paper_price DECIMAL(10, 2);
ALTER TABLE cards ADD COLUMN online_price DECIMAL(10, 2);

CREATE INDEX cards_paper_price_idx ON cards (paper_price);
CREATE INDEX cards_paper_price_nulls_first_idx ON cards (paper_price NULLS FIRST);
CREATE INDEX cards_online_price_idx ON cards (online_price);
CREATE INDEX cards_online_price_nulls_first_idx ON cards (online_price NULLS FIRST);


--
-- trigger to run whenever a card_price row is added/updated that fills in
-- both the online_price and paper_price columns of the corresponding card
--

CREATE OR REPLACE FUNCTION most_recent_card_price()
	RETURNS TRIGGER
AS $most_recent_card_price$
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
		paper_price  = (
			SELECT cp.price
			FROM card_prices cp
			WHERE cp.card_id = update_card_id AND cp.online = FALSE
			ORDER BY cp.last_updated_at DESC
			LIMIT 1
		),
		online_price = (
			SELECT cp.price
			FROM card_prices cp
			WHERE cp.card_id = update_card_id AND cp.online = TRUE
			ORDER BY cp.last_updated_at DESC
			LIMIT 1
		)
	WHERE id = update_card_id;
	RETURN NULL;
END;
$most_recent_card_price$ LANGUAGE plpgsql;

CREATE TRIGGER cards_update_most_recent_card_price_trigger
AFTER INSERT OR UPDATE OR DELETE ON card_prices
FOR EACH ROW EXECUTE PROCEDURE most_recent_card_price();


--
-- fill in paper_price / online_price columns in any existing cards
--

UPDATE cards c
SET
	paper_price  = (
		SELECT cp.price
		FROM card_prices cp
		WHERE cp.card_id = c.id AND cp.online = FALSE
		ORDER BY cp.last_updated_at DESC
		LIMIT 1
	),
	online_price = (
		SELECT cp.price
		FROM card_prices cp
		WHERE cp.card_id = c.id AND cp.online = TRUE
		ORDER BY cp.last_updated_at DESC
		LIMIT 1
	);
