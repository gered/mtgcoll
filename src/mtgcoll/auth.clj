(ns mtgcoll.auth
  (:require
    [mtgcoll.config :as config]))

(defn using-authorization?
  []
  (boolean (seq (config/get :users))))

(defn validate-credentials
  [username password]
  (if (using-authorization?)
    (as-> (config/get :users) x
          (filter #(and (= username (:username %))
                        (= password (:password %))) x)
          (first x)
          (dissoc x :password))))
