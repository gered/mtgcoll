CREATE OR REPLACE FUNCTION update_lists_card_quantities()
	RETURNS TRIGGER
AS $update_lists_card_quantities$
DECLARE
	update_card_id TEXT;
	update_list_id INT;
	new_quantity INT;
	new_foil_quantity INT;
BEGIN
	-- get card_id and list_id of the lists_card_quantities row to insert/update
	IF (TG_OP = 'DELETE') THEN
		update_card_id = OLD.card_id;
		update_list_id = OLD.list_id;
	ELSE
		update_card_id = NEW.card_id;
		update_list_id = NEW.list_id;
	END IF;

	-- get quantities for both foil and non-foil to insert/update in the lists_card_quantities row

	SELECT COALESCE(SUM(cl.quantity), 0) INTO STRICT new_quantity
	FROM collection cl
	WHERE cl.card_id = update_card_id
	      AND cl.list_id = update_list_id;

	SELECT COALESCE(SUM(cl.quantity), 0) INTO STRICT new_foil_quantity
	FROM collection cl
	WHERE cl.card_id = update_card_id
	      AND cl.list_id = update_list_id
	      AND cl.foil = TRUE;

	-- poor man's upsert equivalent
	LOOP
		UPDATE lists_card_quantities
		SET quantity = new_quantity,
			foil_quantity = new_foil_quantity
		WHERE card_id = update_card_id
		      AND list_id = update_list_id;
		IF FOUND THEN
			-- update succeeded (existing row was present). exit loop
			RETURN NULL;
		END IF;

		-- if we get here, an existing row was not present so we should try to insert one
		BEGIN
			INSERT INTO lists_card_quantities (card_id, list_id, quantity, foil_quantity)
			VALUES (update_card_id, update_list_id, new_quantity, new_foil_quantity);
			-- insert succeeded. exit loop
			RETURN NULL;
			EXCEPTION WHEN UNIQUE_VIOLATION THEN
			-- insert failed because another operation finished first.
			-- do nothing and just loop and retry the update
		END;
	END LOOP;
END;
$update_lists_card_quantities$ LANGUAGE plpgsql;
