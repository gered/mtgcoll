(ns mtgcoll.views.functions.collection)

(defn owned-card
  [card-id]
  ["select quality, quantity
    from collection
    where card_id = ?"
   card-id])

(defn total-owned-of-card
  [card-id]
  ["select count(*)
    from collection
    where card_id = ?"
   card-id])
