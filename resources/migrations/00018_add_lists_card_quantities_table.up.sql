CREATE TABLE lists_card_quantities (
	id            SERIAL PRIMARY KEY,
	card_id       TEXT NOT NULL,
	list_id       INT  NOT NULL,
	quantity      INT  NOT NULL DEFAULT 0,   -- this is a total quantity... non-foil + foil combined
	foil_quantity INT  NOT NULL DEFAULT 0,
	UNIQUE (card_id, list_id)
);

ALTER TABLE ONLY lists_card_quantities ADD CONSTRAINT lists_card_quantities_card_id_fkey FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE;
ALTER TABLE ONLY lists_card_quantities ADD CONSTRAINT lists_card_quantities_list_id_fkey FOREIGN KEY (list_id) REFERENCES lists (id) ON DELETE CASCADE;

CREATE INDEX lists_card_quantities_card_id_idx ON lists_card_quantities (card_id);
CREATE INDEX lists_card_quantities_list_id_idx ON lists_card_quantities (list_id);

-- fill in the table with rows for all cards/lists
INSERT INTO lists_card_quantities (card_id, list_id, quantity, foil_quantity)
SELECT
	cl.card_id,
	cl.list_id,
	(
		SELECT COALESCE(SUM(subcl.quantity), 0)
		FROM collection subcl
		WHERE subcl.card_id = cl.card_id
		      AND subcl.list_id = cl.list_id
	) AS quantity,
	(
		SELECT COALESCE(SUM(subcl.quantity), 0)
		FROM collection subcl
		WHERE subcl.card_id = cl.card_id
		      AND subcl.list_id = cl.list_id
		      AND subcl.foil = TRUE
	) AS foil_quantity
FROM
(
	SELECT DISTINCT card_id, list_id
	FROM collection
) cl;
