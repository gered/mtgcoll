DROP TRIGGER IF EXISTS collection_set_online_trigger ON collection;

DROP FUNCTION IF EXISTS set_collection_row_online_column();

ALTER TABLE collection DROP COLUMN IF EXISTS online;
