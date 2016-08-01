(ns mtgcoll.views.functions.collection)

(defn card-inventory
  [card-id list-id user-id]
  (let [list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select cl.quality, cl.quantity, cl.foil
      from collection cl
      join lists l on cl.list_id = l.id
      where cl.card_id = ?
            and cl.list_id = ?
            and (l.is_public in (true, ?))"
     card-id list-id public-only?]))

(defn total-card-inventory
  [card-id list-id user-id]
  (let [list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select count(*)
      from collection cl
      join lists l on cl.list_id = l.id
      where card_id = ?
            and cl.list_id = ?
            and (l.is_public in (true, ?))"
     card-id list-id public-only?]))
