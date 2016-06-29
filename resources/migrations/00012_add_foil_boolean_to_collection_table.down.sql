ALTER TABLE collection DROP COLUMN IF EXISTS foil;

ALTER TABLE collection DROP CONSTRAINT IF EXISTS collection_card_id_quality_foil_key;
ALTER TABLE collection ADD CONSTRAINT collection_card_id_quality_key UNIQUE (card_id, quality);
