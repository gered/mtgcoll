ALTER TABLE collection ADD COLUMN foil BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX collection_foil_idx ON collection (foil);

ALTER TABLE collection DROP CONSTRAINT IF EXISTS collection_card_id_quality_key;
ALTER TABLE collection ADD CONSTRAINT collection_card_id_quality_foil_key UNIQUE (card_id, quality, foil);
