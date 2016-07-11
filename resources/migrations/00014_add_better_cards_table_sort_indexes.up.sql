-- these indexes are to make sure that all the sorting possible via the web UI
-- is fast and _stable_ across multiple pages of the same value
-- (e.g. sorting on rarity and navigating through multiple pages of 'Common' values)
--
-- of course, these additional indexes don't take care of all possible sorting and filtering
-- permutations available through the web UI (still possible to end up with some bitmap scans, etc.
-- in the query plans used in some cases)... but it definitely is better then nothing

CREATE INDEX cards_name_asc_id_idx ON cards (name ASC, id);
CREATE INDEX cards_name_desc_id_idx ON cards (name DESC, id);

CREATE INDEX cards_set_code_asc_id_idx ON cards (set_code ASC, id);
CREATE INDEX cards_set_code_desc_id_idx ON cards (set_code DESC, id);

CREATE INDEX cards_converted_mana_cost_nulls_first_asc_id_idx ON cards (converted_mana_cost ASC NULLS FIRST, id);
CREATE INDEX cards_converted_mana_cost_nulls_last_desc_id_idx ON cards (converted_mana_cost DESC NULLS LAST, id);

CREATE INDEX cards_type_asc_id_idx ON cards (type ASC, id);
CREATE INDEX cards_type_desc_id_idx ON cards (type DESC, id);

CREATE INDEX cards_rarity_asc_id_idx ON cards (rarity ASC, id);
CREATE INDEX cards_rarity_desc_id_idx ON cards (rarity DESC, id);

CREATE INDEX cards_paper_price_nulls_first_asc_id_idx ON cards (paper_price ASC NULLS FIRST, id);
CREATE INDEX cards_paper_price_nulls_last_desc_id_idx ON cards (paper_price DESC NULLS LAST, id);

CREATE INDEX cards_online_price_nulls_first_asc_id_idx ON cards (online_price ASC NULLS FIRST, id);
CREATE INDEX cards_online_price_nulls_last_desc_id_idx ON cards (online_price DESC NULLS LAST, id);

CREATE INDEX cards_owned_count_asc_id_idx ON cards (owned_count ASC, id);
CREATE INDEX cards_owned_count_desc_id_idx ON cards (owned_count DESC, id);
