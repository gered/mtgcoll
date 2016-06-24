(ns mtgcoll.routes.images
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as sql]
    [compojure.core :refer [routes GET POST]]
    [webtools.response :as response]
    [mtgcoll.models.cards :as cards]))

(defn sanitize-path-arg
  [arg]
  (.replaceAll (str arg) "[^A-Za-z0-9_\\-]" ""))

(def image-routes
  (routes
    (GET "/images/cards/:id" [id]
      (if-let [{:keys [image_bytes mimetype]} (cards/get-card-db-image id)]
        (-> (response/content (io/input-stream image_bytes))
            (response/content-type mimetype))
        (-> (response/content (-> (io/resource "public/img/cardback.jpg")
                                  (io/file)))
            (response/content-type "image/jpeg"))))

    (GET "/images/:type/:size/:name" [type size name]
      (let [size (some #{size} ["small" "medium"])
            path (str "public/img/" (sanitize-path-arg type) "/" size "/" (sanitize-path-arg name) ".png")
            res  (io/resource path)]
        (if res
          (io/file res)
          (-> (io/resource (if (= "sets" type)
                             "public/img/empty.png"
                             (str "public/img/missing_symbol_" size ".png")))
              (io/file)))))))
