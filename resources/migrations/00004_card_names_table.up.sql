CREATE TABLE card_names (
	id      SERIAL PRIMARY KEY,
	card_id TEXT NOT NULL,
	name    TEXT NOT NULL
);

CREATE INDEX card_names_name_idx ON card_names (name);

ALTER TABLE ONLY card_names ADD CONSTRAINT card_names_card_id_fkey FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE;
CREATE INDEX card_names_card_id_idx ON card_names (card_id);
