CREATE TABLE card_images (
	id          SERIAL PRIMARY KEY,
	card_id     TEXT NOT NULL,
	image_name  TEXT NOT NULL,
	image_bytes BYTEA,
	mimetype    TEXT
);

ALTER TABLE ONLY card_images ADD CONSTRAINT card_images_card_id_fkey FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE;
CREATE INDEX card_images_card_id_idx ON card_images (card_id);
