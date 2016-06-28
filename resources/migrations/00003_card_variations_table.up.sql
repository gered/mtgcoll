CREATE TABLE card_variations (
	id              SERIAL PRIMARY KEY,
	card_id         TEXT NOT NULL,
	multiverseid    INT  NOT NULL,
	variant_card_id TEXT
);

CREATE INDEX card_variations_multiverseid_idx ON card_variations (multiverseid);
CREATE INDEX card_variations_variant_card_id_idx ON card_variations (variant_card_id);

ALTER TABLE ONLY card_variations ADD CONSTRAINT card_variations_card_id_fkey FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE;
CREATE INDEX card_variations_card_id_idx ON card_variations (card_id);
