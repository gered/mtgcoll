CREATE TABLE sets (
	code                  TEXT PRIMARY KEY,
	name                  TEXT    NOT NULL,
	gatherer_code         TEXT,
	old_code              TEXT,
	magic_cards_info_code TEXT,
	release_date          DATE,
	border                TEXT    NOT NULL CHECK (border IN ('white', 'black', 'silver')),
	type                  TEXT    NOT NULL,
	block                 TEXT,
	online_only           BOOLEAN NOT NULL
);

CREATE INDEX sets_name_idx ON sets (name);
CREATE INDEX sets_gatherer_code_idx ON sets (gatherer_code);
CREATE INDEX sets_magic_cards_info_code_idx ON sets (magic_cards_info_code);
CREATE INDEX sets_release_date_idx ON sets (release_date);
CREATE INDEX sets_type_idx ON sets (type);
