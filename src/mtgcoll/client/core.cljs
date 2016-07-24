(ns mtgcoll.client.core
  (:require
    [reagent.core :as r]
    [secretary.core :refer-macros [defroute]]
    [webtools.cljs.ajax :as ajax]
    [webtools.cljs.utils :refer [hook-browser-navigation!]]
    [mtgcoll.client.views :as views]
    [mtgcoll.client.page :as page]
    [mtgcoll.client.routes.cards :as cards]
    [mtgcoll.client.routes.collection :as collection]
    [mtgcoll.client.routes.sets :as sets]
    [mtgcoll.client.routes.stats :as stats]))

(defroute "/" [] (page/page [collection/owned-cards-list]))
(defroute "/owned" [] (page/page [collection/owned-cards-list]))
(defroute "/all" [] (page/page [collection/all-cards-list]))
(defroute "/sets" [] (page/page [sets/sets-list]))
(defroute "/set/:code" [code] (page/page [sets/set-details code]))
(defroute "/card/:id" [id] (page/page [cards/card-details id 0]))
(defroute "/stats" [] (page/page [stats/stats-page]))
(defroute "*" [] (page/barebones-page [:div "not found"]))

(defn ^:export run
  []
  (enable-console-print!)
  (ajax/add-csrf-header!)
  (views/init!)
  (hook-browser-navigation!))

(run)
