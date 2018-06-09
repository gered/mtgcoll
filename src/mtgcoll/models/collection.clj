(ns mtgcoll.models.collection
  (:require
    [clojure.java.jdbc :as jdbc]
    [views.core :as views]
    [views.sql.core :as vsql :refer [vexec! with-view-transaction]]
    [mtgcoll.db :refer [db]]
    [mtgcoll.views.core :refer [view-system]]))

(defn- update-collection!*
  [db card-id quality foil? list-id quantity-change]
  ;; written assuming postgresql server is _not_ 9.5+ (so, without access to UPSERT functionality)
  (let [num-updates (first
                      ; i love that SQL forces you to use "is null" ...
                      (if (nil? quality)
                        (jdbc/execute!
                          db
                          ["update collection
                            set quantity = quantity + ?
                            where card_id = ? and
                                  quality is null and
                                  foil = ? and
                                  list_id = ?"
                           quantity-change card-id foil? list-id])
                        (jdbc/execute!
                          db
                          ["update collection
                            set quantity = quantity + ?
                            where card_id = ? and
                                  quality = ? and
                                  foil = ? and
                                  list_id = ?"
                           quantity-change card-id quality foil? list-id])))]
    (if (= 0 num-updates)
      (first
        (jdbc/execute!
          db
          ["insert into collection
            (card_id, quality, quantity, foil, list_id)
            values
            (?, ?, ?, ?, ?)"
           card-id quality quantity-change foil? list-id]))
      num-updates)))

(defn update-collection!
  [card-id quality foil? list-id user-id quantity-change]
  (let [list-id      (int list-id)
        public-only? (nil? user-id)]
    (jdbc/with-db-transaction
      [dt db]
      (if-not (first (jdbc/query dt ["select count(*) from lists where id = ? and is_public in (true, ?)" list-id public-only?]))
        (throw (new Exception (str "Not authorized to update list:" list-id)))
        (update-collection!* dt card-id quality foil? list-id quantity-change)))
    (views/put-hints! view-system [(views/hint nil #{:collection} vsql/hint-type)])))

(defn add-to-collection!
  [card-id quality foil? list-id user-id]
  (update-collection! card-id quality foil? list-id user-id 1))

(defn remove-from-collection!
  [card-id quality foil? list-id user-id]
  (update-collection! card-id quality foil? list-id user-id -1))

(defn copy-list!
  [source-list-id destination-list-id user-id]
  (let [source-list-id      (int source-list-id)
        destination-list-id (int destination-list-id)]
    (when (= source-list-id destination-list-id)
      (throw (Exception. "Cannot copy list onto itself.")))
    (jdbc/with-db-transaction
      [dt db]
      (let [source-has-qualities? (first (jdbc/query dt ["select require_qualities from lists where id = ?" source-list-id] {:row-fn :require_qualities}))
            dest-has-qualities? (first (jdbc/query dt ["select require_qualities from lists where id = ?" destination-list-id] {:row-fn :require_qualities}))]
        (when (not (or (and (not source-has-qualities?)
                            (not dest-has-qualities?))
                       source-has-qualities?))
          (throw (Exception. "Invalid copy. 'Require qualities?' compatibility mismatch.")))
        (let [source-data (jdbc/query dt ["select card_id, quality, quantity, foil from collection where list_id = ?" source-list-id])]
          (doseq [{:keys [card_id quality quantity foil]} source-data]
            (update-collection!* dt card_id (if dest-has-qualities? quality) foil destination-list-id quantity)))))
    (views/put-hints! view-system
                      [(views/hint nil #{:collection} vsql/hint-type)
                       (views/hint nil #{:lists} vsql/hint-type)])
    true))
