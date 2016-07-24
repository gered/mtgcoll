(ns mtgcoll.models.collection
  (:require
    [clojure.java.jdbc :as jdbc]
    [views.sql.core :refer [vexec! with-view-transaction]]
    [mtgcoll.db :refer [db]]
    [mtgcoll.views.core :refer [view-system]]))

(defn update-collection!
  [card-id quality foil? list-id user-id quantity-change]
  ;; written assuming postgresql server is _not_ 9.5+ (so, without access to UPSERT functionality)
  (let [list-id      (int list-id)
        public-only? (nil? user-id)]
    (with-view-transaction
      view-system
      [dt @db]
      (if-not (first (jdbc/query dt ["select count(*) from lists where id = ? and is_public in (true, ?)" list-id public-only?]))
        (throw (new Exception (str "Not authorized to update list:" list-id)))
        (let [num-updates (first
                            (vexec! view-system dt
                                    ["update collection
                                      set quantity = quantity + ?
                                      where card_id = ? and
                                            quality = ? and
                                            foil = ? and
                                            list_id = ?"
                                     quantity-change card-id quality foil? list-id]))]
          (if (= 0 num-updates)
            (first
              (vexec! view-system dt
                      ["insert into collection
                        (card_id, quality, quantity, foil, list_id)
                        values
                        (?, ?, ?, ?, ?)"
                       card-id quality quantity-change foil? list-id]))
            num-updates))))))

(defn add-to-collection!
  [card-id quality foil? list-id user-id]
  (update-collection! card-id quality foil? list-id user-id 1))

(defn remove-from-collection!
  [card-id quality foil? list-id user-id]
  (update-collection! card-id quality foil? list-id user-id -1))
