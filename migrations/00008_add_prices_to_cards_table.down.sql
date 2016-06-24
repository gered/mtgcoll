DROP TRIGGER IF EXISTS cards_update_most_recent_card_price_trigger ON card_prices;

DROP FUNCTION IF EXISTS most_recent_card_price();

ALTER TABLE cards DROP COLUMN IF EXISTS paper_price;
ALTER TABLE cards DROP COLUMN IF EXISTS online_price;
