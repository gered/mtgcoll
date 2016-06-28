CREATE TABLE card_prices (
	id              SERIAL PRIMARY KEY,
	card_id         TEXT           NOT NULL,
	source          TEXT           NOT NULL,
	online          BOOLEAN        NOT NULL,
	price           DECIMAL(10, 2) NOT NULL,
	last_updated_at TIMESTAMP      NOT NULL,
	UNIQUE (card_id, source, online)
);

ALTER TABLE ONLY card_prices ADD CONSTRAINT card_prices_card_id_fkey FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE;
CREATE INDEX card_prices_card_id_idx ON card_prices (card_id);
CREATE INDEX card_prices_online_idx ON card_prices (online);
