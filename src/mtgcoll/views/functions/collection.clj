(ns mtgcoll.views.functions.collection)

(defn owned-card
  [card-id]
  ["select quality, quantity, foil
    from collection
    where card_id = ? AND list_id = 0"
   card-id])

(defn total-owned-of-card
  [card-id]
  ["select count(*)
    from collection
    where card_id = ? AND list_id = 0"
   card-id])
