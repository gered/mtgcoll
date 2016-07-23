CREATE TABLE lists (
	id                SERIAL PRIMARY KEY,
	name              TEXT    NOT NULL,
	notes             TEXT,
	is_public         BOOLEAN NOT NULL DEFAULT FALSE,
	require_qualities BOOLEAN NOT NULL DEFAULT FALSE,
	UNIQUE (name)
);

CREATE INDEX lists_name_idx ON lists (name);

INSERT INTO lists (id, name, is_public, require_qualities) VALUES (0, 'Owned', TRUE, TRUE);
