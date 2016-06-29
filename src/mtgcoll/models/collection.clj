(ns mtgcoll.models.collection
  (:require
    [views.sql.core :refer [vexec! with-view-transaction]]
    [mtgcoll.db :refer [db]]
    [mtgcoll.views.core :refer [view-system]]))

(defn update-collection!
  [card-id quality foil? quantity-change]
  ;; written assuming postgresql server is _not_ 9.5+ (so, without access to UPSERT functionality)
  (with-view-transaction
    view-system
    [dt @db]
    (let [num-updates (first
                        (vexec! view-system dt
                                ["update collection
                                  set quantity = quantity + ?
                                  where card_id = ? and
                                        quality = ? and
                                        foil = ?"
                                 quantity-change card-id quality foil?]))]
      (if (= 0 num-updates)
        (first
          (vexec! view-system dt
                  ["insert into collection
                    (card_id, quality, quantity, foil)
                    values
                    (?, ?, ?, ?)"
                   card-id quality quantity-change foil?]))
        num-updates))))

(defn add-to-collection!
  [card-id quality foil?]
  (update-collection! card-id quality foil? 1))

(defn remove-from-collection!
  [card-id quality foil?]
  (update-collection! card-id quality foil? -1))
