(ns mtgcoll.routes.main-page
  (:require
    [clojure.string :as string]
    [compojure.core :refer [routes GET POST]]
    [ring.util.anti-forgery :refer [anti-forgery-field]]
    [hiccup.page :refer [include-css include-js]]
    [hiccup.element :refer [javascript-tag]]
    [webtools.page :refer [html5 js-env-settings]]
    [mtgcoll.config :as config])
  (:use
    mtgcoll.utils))

(defn main-page
  [request]
  (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     (anti-forgery-field)
     [:title "MTG Web Collection"]
     (include-css "/assets/bootstrap/css/bootstrap.min.css")
     (include-css "/assets/bootstrap/css/bootstrap-theme.min.css")
     (include-css "css/app.css")]
    [:body
     [:div#wrap
      [:div#app [:h1 "Please wait, loading ..."]]
      [:div#push]]
     [:div#footer
      [:div.container-fluid
       [:div.content.text-center.text-muted
        "mtgcoll " (get-app-version)
        "&nbsp; | &nbsp;"
        [:a {:href "https://github.com/gered/mtgcoll"} "GitHub Project"]]]]
     (js-env-settings "" (boolean (config/get :dev?)))
     (javascript-tag
       (string/join "\n" [(str "var __authRequired = " (boolean (seq (config/get :users))) ";")]))
     (if-let [default-price-source (config/get :default-price-source)]
       (javascript-tag (str "var __defaultPriceSource = '" default-price-source "';")))
     (include-js "cljs/app.js")]))

(def main-page-routes
  (routes
    (GET "/" request (main-page request))))
