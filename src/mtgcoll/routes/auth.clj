(ns mtgcoll.routes.auth
  (:require
    [clojure.tools.logging :as log]
    [compojure.core :refer [routes GET POST]]
    [webtools.response :as response]
    [webtools.session :as session]
    [mtgcoll.config :as config]))

(def auth-routes
  (routes
    (POST "/login" [username password :as request]
      (if-let [user (->> (config/get :users)
                         (filter #(and (= username (:username %))
                                       (= password (:password %))))
                         (first))]
        (do
          (log/info username " logged in.")
          (-> (response/content "ok")
              (session/set-from-request request)
              (session/assoc :user user)))
        (do
          (log/warn "Unsuccessful login attempt by: " username)
          (-> (response/content "bad username/password")
              (response/status 401)))))

    (POST "/logout" request
      (if-let [user (get-in request [:session :user])]
        (do
          (log/info (:username user) " logged out.")
          (-> (response/content "ok")
              (session/set-from-request request)
              (session/dissoc :user)))
        (do
          (-> (response/content "not logged in")
              (response/status 400)))))))
