CREATE TABLE cards (
	id                  TEXT PRIMARY KEY,
	set_code            TEXT    NOT NULL,
	layout              TEXT    NOT NULL CHECK (layout IN
	                                            ('normal', 'split', 'flip', 'double-faced', 'token', 'plane', 'scheme', 'phenomenon', 'leveler', 'vanguard')),
	name                TEXT    NOT NULL,
	normalized_name     TEXT    NOT NULL,
	mana_cost           TEXT,
	converted_mana_cost DECIMAL(10, 2),
	colors              TEXT, -- one or more colors (comma separated)
	color_identity      TEXT, -- one or more colors (comma separated)
	type                TEXT    NOT NULL,
	supertypes          TEXT, -- one or more supertypes (comma separated)
	types               TEXT, -- one or more types (comma separated)
	subtypes            TEXT, -- one or more subtypes (comma separated)
	rarity              TEXT    NOT NULL,
	text                TEXT,
	flavor              TEXT,
	artist              TEXT    NOT NULL,
	number              TEXT,
	power               TEXT,
	toughness           TEXT,
	loyalty             INT,
	multiverseid        INT,
	image_name          TEXT,
	watermark           TEXT,
	border              TEXT CHECK (border IN (NULL, 'white', 'black', 'silver')),
	timeshifted         BOOLEAN NOT NULL,
	hand                INT,
	life                INT,
	reserved            BOOLEAN NOT NULL,
	release_date        TEXT, -- may be YYYY-MM-DD, YYYY-MM, or just YYYY (hence why TEXT and not DATE)
	starter             BOOLEAN NOT NULL
);

CREATE INDEX cards_name_idx ON cards (name);
CREATE INDEX cards_normalized_name_idx ON cards (normalized_name);
CREATE INDEX cards_converted_mana_cost_idx ON cards (converted_mana_cost);
CREATE INDEX cards_converted_mana_cost_nulls_first_idx ON cards (converted_mana_cost NULLS FIRST);
CREATE INDEX cards_multiverseid_idx ON cards (multiverseid);
CREATE INDEX cards_colors_idx ON cards (colors);
CREATE INDEX cards_color_identity_idx ON cards (color_identity);
CREATE INDEX cards_type_idx ON cards (type);
CREATE INDEX cards_supertypes_idx ON cards (supertypes);
CREATE INDEX cards_types_idx ON cards (types);
CREATE INDEX cards_subtypes_idx ON cards (subtypes);
CREATE INDEX cards_rarity_idx ON cards (rarity);
CREATE INDEX cards_artist_idx ON cards (artist);

ALTER TABLE ONLY cards ADD CONSTRAINT cards_set_code_fkey FOREIGN KEY (set_code) REFERENCES sets (code) ON DELETE CASCADE;
CREATE INDEX cards_set_code_idx ON cards (set_code);
