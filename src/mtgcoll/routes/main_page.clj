(ns mtgcoll.routes.main-page
  (:require
    [clojure.string :as string]
    [compojure.core :refer [routes GET POST]]
    [hiccup.page :refer [include-css include-js]]
    [hiccup.element :refer [javascript-tag]]
    [webtools.page :refer [html5 js-env-settings]]
    [webtools.reagent.page :refer [include-bootstrap-metatags include-bootstrap-css]]
    [mtgcoll.config :as config])
  (:use
    mtgcoll.utils))

(defn main-page
  [request]
  (html5
    [:head
     [:title "MTG Web Collection"]
     (include-bootstrap-metatags)
     (include-bootstrap-css true)
     (include-css "css/app.css")]
    [:body
     [:div#wrap
      [:div#app [:h1 "Please wait, loading ..."]]
      [:div#push]]
     [:div#footer
      [:div.container-fluid
       [:div.content.text-center.text-muted
        "mtgcoll " (get-app-version) "&nbsp; | &nbsp;"
        [:a {:href "https://github.com/gered/mtgcoll"} "Source code"] " licensed under MIT."]
        ]]
     (js-env-settings "" (boolean (config/get :dev?)))
     (include-js "cljs/app.js")]))

(def main-page-routes
  (routes
    (GET "/" request (main-page request))))
