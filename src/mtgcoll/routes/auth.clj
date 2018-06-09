(ns mtgcoll.routes.auth
  (:require
    [clojure.tools.logging :as log]
    [compojure.core :refer [routes GET POST]]
    [webtools.response :as response]
    [webtools.routes.core :refer [wrap-middleware]]
    [webtools.session :as session]
    [mtgcoll.middleware :refer [wrap-api-exceptions wrap-authenticated]]
    [mtgcoll.auth :as auth]))

(def auth-routes
  (wrap-middleware
    (routes
      (POST "/login" [username password :as request]
        (if-let [user (auth/validate-credentials username password)]
          (do
            (log/info username " logged in.")
            (-> (response/json user)
                (session/set-from-request request)
                (session/assoc :user user)))
          (do
            (log/warn "Unsuccessful login attempt by: " username)
            (-> (response/json {:status  "unauthorized"
                                :message "bad username/password"})
                (response/status 401)))))

      (POST "/logout" request
        (if-let [user (get-in request [:session :user])]
          (do
            (log/info (:username user) " logged out.")
            (-> (response/content "ok")
                (session/set-from-request request)
                (session/dissoc :user)))
          (do
            (-> (response/json {:status  "error"
                                :message "not logged in"})
                (response/status 400))))))

    (wrap-api-exceptions)))
