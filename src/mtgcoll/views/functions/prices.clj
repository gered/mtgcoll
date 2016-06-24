(ns mtgcoll.views.functions.prices)

(defn card-pricing
  [card-id]
  ["select source, online, price, last_updated_at
    from card_prices
    where card_id = ?
    order by last_updated_at desc" card-id])

(defn pricing-sources
  []
  ["select distinct source
    from card_prices
    order by source"])