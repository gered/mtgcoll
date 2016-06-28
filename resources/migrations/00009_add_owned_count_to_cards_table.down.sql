DROP TRIGGER IF EXISTS cards_update_owned_count_trigger ON collection;

DROP FUNCTION IF EXISTS update_card_owned_count();

ALTER TABLE cards DROP COLUMN IF EXISTS owned_count;
