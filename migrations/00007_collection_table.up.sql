CREATE TABLE collection (
	id       SERIAL PRIMARY KEY,
	card_id  TEXT    NOT NULL,
	quality  TEXT    NOT NULL CHECK (quality IN
	                                 ('online', 'near mint', 'lightly played', 'moderately played', 'heavily played', 'damaged')),
	quantity INT     NOT NULL CHECK (quantity >= 0),
	UNIQUE (card_id, quality)
);

ALTER TABLE ONLY collection ADD CONSTRAINT collection_card_id_fkey FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE;
CREATE INDEX collection_card_id_idx ON collection (card_id);
