(ns mtgcoll.routes.collection
  (:require
    [compojure.core :refer [routes GET POST]]
    [webtools.response :as response]
    [webtools.routes.core :refer [wrap-middleware]]
    [mtgcoll.middleware :refer [wrap-authenticated]]
    [mtgcoll.models.collection :as collection]))

(def collection-routes
  (wrap-middleware
    (routes
      (POST "/collection/add" [card-id quality foil list-id :as request]
        (let [username (get-in request [:session :user :username])
              result   (collection/add-to-collection! card-id quality foil list-id username)]
          (response/json {:status "ok"})))

      (POST "/collection/remove" [card-id quality foil list-id :as request]
        (let [username (get-in request [:session :user :username])
              result   (collection/remove-from-collection! card-id quality foil list-id username)]
          (response/json {:status "ok"})))

      (POST "/collection/copy-list" [source-list-id destination-list-id :as request]
        (let [username (get-in request [:session :user :username])
              result   (collection/copy-list! source-list-id destination-list-id username)]
          (response/json {:status "ok"}))))
    (wrap-authenticated)))
