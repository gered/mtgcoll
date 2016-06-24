(ns mtgcoll.routes.main-page
  (:require
    [compojure.core :refer [routes GET POST]]
    [hiccup.page :refer [include-css include-js]]
    [hiccup.element :refer [javascript-tag]]
    [webtools.page :refer [html5 js-env-settings]]
    [webtools.reagent.page :refer [include-bootstrap-metatags include-bootstrap-css]]
    [config.core :as config]
    [mtgcoll.config :refer [config]]))

(defn main-page
  [request]
  (html5
    [:head
     [:title "MTG Web Collection"]
     (include-bootstrap-metatags)
     (include-bootstrap-css true)
     (include-css "css/app.css")]
    [:body
     [:div#app [:h1 "Please wait, loading ..."]]
     (js-env-settings "" (boolean (config/get config :dev?)))
     (include-js "cljs/app.js")]))

(def main-page-routes
  (routes
    (GET "/" request (main-page request))))
