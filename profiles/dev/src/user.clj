(ns user
  (:use
    mtgcoll.core)
  (:require
    [mount.core :as mount]
    [ragtime.repl :as ragtime]
    [mtgcoll.cli :as cli]
    [mtgcoll.config :as config]
    [mtgcoll.db :as db]))

(defn migrate [& args]
  (let [{:keys [errors] :as args} (cli/parse-args args)]
    (when errors
      (cli/show-error! errors)
      (System/exit 1))
    (mount/start-with-args args #'config/app-config #'db/db)
    (println "Running migrations on" (:subname db/db))
    (ragtime/migrate (db/get-ragtime-config))))

(defn rollback [& args]
  (let [{:keys [errors] :as args} (cli/parse-args args)]
    (when errors
      (cli/show-error! errors)
      (System/exit 1))
    (mount/start-with-args args #'config/app-config #'db/db)
    (println "Rolling back migrations on" (:subname db/db))
    (ragtime/rollback (db/get-ragtime-config) (or (first args) 1))))
