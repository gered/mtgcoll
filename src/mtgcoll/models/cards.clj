(ns mtgcoll.models.cards
  (:require
    [clojure.java.jdbc :as sql]
    [honeysql.core :as hsql]
    [mtgcoll.db :refer [db]]))

(defn get-card-image-info
  [card-id]
  (sql/query db ["select set_code, image_name
                  from cards
                  where id = ?" card-id]
             {:result-set-fn first}))

(defn get-matching-card-ids
  [card-name set-code & [{:keys [split? normalized-name? number]}]]
  (let [q {:select [:id]
           :from   [:cards]
           :where  (as-> [:and] x
                         (conj x [:= :set_code set-code])
                         (conj x (if normalized-name?
                                   [:= :normalized_name card-name]
                                   [:= :name card-name]))
                         (conj x (if split?
                                   [:= :layout "split"]))
                         (conj x (if number
                                   [:= :number number]))
                         (remove nil? x)
                         (vec x))}]
    (seq (sql/query db (hsql/format q) {:row-fn :id}))))

(defn get-card-id-by-multiverse
  [multiverse-id set-code & [card-name split?]]
  (seq
    (if split?
      (sql/query db ["select id
                      from cards
                      where multiverseid = ?
                            and set_code = ?
                            and name = ?
                            and layout = 'split'"
                     multiverse-id set-code card-name]
                 {:row-fn :id})
      (sql/query db ["select id
                      from cards
                      where multiverseid = ?
                            and set_code = ?"
                     multiverse-id set-code]
                 {:row-fn :id}))))

(defn update-price!
  [card-id price-source price online?]
  ;; written assuming postgresql server is _not_ 9.5+ (so, without access to UPSERT functionality)
  (sql/with-db-transaction
    [dt db]
    (let [num-updates (first
                        (sql/execute!
                          dt
                          ["update card_prices
                            set price = ?,
                                last_updated_at = current_timestamp
                            where card_id = ? and
                                  source = ? and
                                  online = ?"
                           price card-id (name price-source) online?]))]
      (if (= 0 num-updates)
        (first
          (sql/execute!
            dt
            ["insert into card_prices
              (card_id, source, online, price, last_updated_at)
              values
              (?, ?, ?, ?, current_timestamp)"
             card-id (name price-source) online? price]))
        num-updates))))

(defn update-prices!
  [price-source prices & [{:keys [normalized-name? multiverse-id?]}]]
  (doseq [{:keys [card-name online? set-code price multiverseid split?] :as card-price} prices]
    (if price
      (if-let [card-ids (if multiverse-id?
                          (get-card-id-by-multiverse multiverseid set-code card-name split?)
                          (get-matching-card-ids card-name set-code
                                                 {:split?           split?
                                                  :normalized-name? normalized-name?}))]
        (doseq [card-id card-ids]
          (update-price! card-id price-source price online?))
        (println "no card match found for:" card-name "," set-code))
      (println "skipping over null price for:" card-name "," set-code))))
