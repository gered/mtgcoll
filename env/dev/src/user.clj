(ns user
  (:use
    mtgcoll.core)
  (:require
    [ragtime.jdbc :as jdbc]
    [ragtime.repl :as ragtime]
    [mtgcoll.db :as db]))

(defn get-ragtime-config []
  {:datastore  (jdbc/sql-database db/db)
   :migrations (jdbc/load-directory "migrations")})

(defn migrate [& args]
  (println "Running migrations on" (:subname db/db))
  (ragtime/migrate (get-ragtime-config)))

(defn rollback [& args]
  (println "Rolling back migrations on" (:subname db/db))
  (ragtime/rollback (get-ragtime-config) (or (first args) 1)))
