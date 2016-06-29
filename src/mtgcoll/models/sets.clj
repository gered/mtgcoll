(ns mtgcoll.models.sets
  (:require
    [clojure.java.jdbc :as sql]
    [mtgcoll.db :refer [db]]))

(defn get-set-codes
  []
  (seq (sql/query @db ["select code, gatherer_code from sets order by code"])))
