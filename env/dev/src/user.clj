(ns user
  (:use
    mtgcoll.core)
  (:require
    [ragtime.repl :as ragtime]
    [mtgcoll.config :as config]
    [mtgcoll.db :as db]))

(defn migrate [& args]
  (config/load! "config.edn")
  (println "Running migrations on" (:subname @db/db))
  (ragtime/migrate (db/get-ragtime-config)))

(defn rollback [& args]
  (config/load! "config.edn")
  (println "Rolling back migrations on" (:subname @db/db))
  (ragtime/rollback (db/get-ragtime-config) (or (first args) 1)))
