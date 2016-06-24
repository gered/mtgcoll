(ns mtgcoll.db
  (:require
    [clojure.java.jdbc :as sql]
    [config.core :as config]
    [mtgcoll.config :refer [config]]))

(def db {:classname   "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname     (str "//" (config/get config :db :host) ":" (or (config/get config :db :port) 5432) "/" (config/get config :db :name))
         :user        (config/get config :db :username)
         :password    (config/get config :db :password)})

(defn verify-connection
  []
  (sql/query db "select 1"))
