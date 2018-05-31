(ns mtgcoll.routes.main-page
  (:require
    [clojure.string :as string]
    [compojure.core :refer [routes GET POST]]
    [ring.util.anti-forgery :refer [anti-forgery-field]]
    [hiccup.page :refer [include-css include-js]]
    [hiccup.element :refer [javascript-tag]]
    [webtools.page :refer [html5 js-env-settings]]
    [webtools.reagent.page :refer [include-bootstrap-metatags]]
    [mtgcoll.config :as config])
  (:use
    mtgcoll.utils))

(defn- include-bootstrap-css
  [& [use-bootstrap-theme?]]
  (->> ["https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
        (if use-bootstrap-theme? "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css")
        "https://npmcdn.com/react-bootstrap-datetimepicker/css/bootstrap-datetimepicker.min.css"
        "https://npmcdn.com/react-select/dist/react-select.min.css"]
       (remove nil?)
       (apply include-css)))

(defn main-page
  [request]
  (html5
    [:head
     [:title "MTG Web Collection"]
     (anti-forgery-field)
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
        "mtgcoll " (get-app-version)
        "&nbsp; | &nbsp;"
        [:a {:href "https://github.com/gered/mtgcoll"} "GitHub Project"]]]]
     (js-env-settings "" (boolean (config/get :dev?)))
     (javascript-tag
       (string/join "\n" [(str "var __authRequired = " (boolean (seq (config/get :users))))]))
     (include-js "cljs/app.js")]))

(def main-page-routes
  (routes
    (GET "/" request (main-page request))))
