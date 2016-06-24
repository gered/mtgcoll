(ns mtgcoll.routes.collection
  (:require
    [compojure.core :refer [routes GET POST]]
    [webtools.response :as response]
    [mtgcoll.models.collection :as collection]))

(def collection-routes
  (routes
    (POST "/collection/add" [card-id quality]
      (collection/add-to-collection! card-id quality)
      (response/json {:status "ok"}))

    (POST "/collection/remove" [card-id quality]
      (collection/remove-from-collection! card-id quality)
      (response/json {:status "ok"}))))
