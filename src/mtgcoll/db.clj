(ns mtgcoll.db
  (:require
    [clojure.java.jdbc :as sql]
    [clojure.tools.logging :as log]
    [mount.core :refer [defstate]]
    [ragtime.jdbc :as jdbc]
    [ragtime.repl :as ragtime]
    [mtgcoll.config :as config]))

(defstate db
  :start (let [db {:classname   "org.postgresql.Driver"
                   :subprotocol "postgresql"
                   :subname     (str "//" (config/get :db :host) ":" (or (config/get :db :port) 5432) "/" (config/get :db :name))
                   :user        (config/get :db :username)
                   :password    (config/get :db :password)}]
           (log/info "Using DB: " (:subname db))
           (sql/query db "select 1")
           db)
  :stop (do
          (log/info "Stopping DB: " (:subname db))
          nil))

(defn get-ragtime-config []
  {:datastore  (jdbc/sql-database db)
   :migrations (jdbc/load-resources "migrations")})

(defn initialize-database!
  []
  (println "Initializating database")
  (ragtime/migrate (get-ragtime-config)))