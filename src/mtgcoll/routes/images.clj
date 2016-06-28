(ns mtgcoll.routes.images
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as sql]
    [compojure.core :refer [routes GET POST]]
    [webtools.response :as response]
    [mtgcoll.config :as config]
    [mtgcoll.models.cards :as cards])
  (:use
    mtgcoll.utils))

(defn sanitize-path-arg
  [arg]
  (.replaceAll (str arg) "[^A-Za-z0-9_\\-]" ""))

(def image-routes
  (routes
    (GET "/images/cards/:id" [id]
      (if-let [{:keys [set_code image_name]} (cards/get-card-image-info id)]
        (let [filename   (str (config/get :card-images-path) "/" set_code "/" image_name ".jpg")
              image-file (if (file-exists? filename)
                           (io/file filename)
                           (io/file (io/resource "public/img/cardback.jpg")))]
          (-> (io/input-stream image-file)
              (response/content)
              (response/content-type "image/jpeg")))))

    (GET "/images/:type/:size/:name" [type size name]
      (let [size (some #{size} ["small" "medium" "large"])
            path (str (config/get :other-images-path) "/" (sanitize-path-arg type) "/" size "/" (sanitize-path-arg name) ".png")
            file (if (file-exists? path)
                   (io/file path)
                   (io/file (io/resource (str "public/img/missing_symbol_" size ".png"))))]
        file))))
