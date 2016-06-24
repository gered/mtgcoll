ALTER TABLE collection ADD COLUMN online BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX collection_online_idx ON collection (online);

--
-- trigger to run whenever a collection row is added/updated that fills in
-- the online column in that collection row
--

CREATE OR REPLACE FUNCTION set_collection_row_online_column()
	RETURNS TRIGGER
AS $set_collection_row_online_column$
BEGIN
	IF NEW.quality = 'online' THEN
		NEW.online := TRUE;
	ELSE
		NEW.online := FALSE;
	END IF;

	RETURN NEW;
END;
$set_collection_row_online_column$ LANGUAGE plpgsql;

CREATE TRIGGER collection_set_online_trigger
BEFORE INSERT OR UPDATE ON collection
FOR EACH ROW EXECUTE PROCEDURE set_collection_row_online_column();


--
-- fill in correct online column value in any existing collection rows
--

UPDATE collection
SET online = TRUE
WHERE quality = 'online';
