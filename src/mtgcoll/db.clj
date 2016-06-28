(ns mtgcoll.db
  (:require
    [clojure.java.jdbc :as sql]
    [ragtime.jdbc :as jdbc]
    [ragtime.repl :as ragtime]
    [mtgcoll.config :as config]))

(defonce db (atom nil))

(defn setup-config!
  []
  (reset! db {:classname   "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname     (str "//" (config/get :db :host) ":" (or (config/get :db :port) 5432) "/" (config/get :db :name))
              :user        (config/get :db :username)
              :password    (config/get :db :password)}))

(defn verify-connection
  []
  (sql/query @db "select 1"))

(defn get-ragtime-config []
  {:datastore  (jdbc/sql-database @db)
   :migrations (jdbc/load-resources "migrations")})

(defn initialize-database!
  []
  (println "Initializating database")
  (ragtime/migrate (get-ragtime-config)))