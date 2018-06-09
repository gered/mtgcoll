(ns mtgcoll.routes.lists
  (:require
    [compojure.core :refer [routes GET POST]]
    [webtools.response :as response]
    [webtools.routes.core :refer [wrap-middleware]]
    [mtgcoll.middleware :refer [wrap-api-exceptions wrap-authenticated]]
    [mtgcoll.models.lists :as lists]))

(def list-routes
  (wrap-middleware
    (routes
      (POST "/lists/add" [name public? requires-qualities? :as request]
        (let [result (lists/add-list! name public? requires-qualities?)]
          (response/json
            {:status "ok"
             :id     result})))

      (POST "/lists/remove" [list-id :as request]
        (lists/remove-list! list-id)
        (response/json {:status "ok"}))

      (POST "/lists/update-name" [list-id name :as request]
        (lists/update-list-name! list-id name)
        (response/json {:status "ok"}))

      (POST "/lists/update-note" [list-id note :as request]
        (lists/update-list-note! list-id note)
        (response/json {:status "ok"}))

      (POST "/lists/update-visibility" [list-id public? :as request]
        (lists/update-list-visibility! list-id public?)
        (response/json {:status "ok"})))

    (wrap-api-exceptions)
    (wrap-authenticated)))