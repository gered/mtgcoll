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
      (POST "/collection/add" [card-id quality foil :as request]
        (collection/add-to-collection! card-id quality foil)
        (response/json {:status "ok"}))

      (POST "/collection/remove" [card-id quality foil :as request]
        (collection/remove-from-collection! card-id quality foil)
        (response/json {:status "ok"})))
    (wrap-authenticated)))
