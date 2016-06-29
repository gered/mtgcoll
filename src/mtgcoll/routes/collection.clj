(ns mtgcoll.routes.collection
  (:require
    [compojure.core :refer [routes GET POST]]
    [webtools.response :as response]
    [mtgcoll.models.collection :as collection]))

(def collection-routes
  (routes
    (POST "/collection/add" [card-id quality foil]
      (collection/add-to-collection! card-id quality foil)
      (response/json {:status "ok"}))

    (POST "/collection/remove" [card-id quality foil]
      (collection/remove-from-collection! card-id quality foil)
      (response/json {:status "ok"}))))
