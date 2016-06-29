(ns mtgcoll.auth
  (:require
    [mtgcoll.config :as config]))

(defn using-authorization?
  []
  (boolean (seq (config/get :users))))

(defn validate-credentials
  [username password]
  (if (using-authorization?)
    (->> (config/get :users)
         (filter #(and (= username (:username %))
                       (= password (:password %))))
         (first))))
