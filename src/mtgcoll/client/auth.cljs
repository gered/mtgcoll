(ns mtgcoll.client.auth
  (:require
    [reagent.core :as r]
    [webtools.cljs.utils :refer [->url]]
    [webtools.cljs.ajax :as ajax]
    [mtgcoll.client.utils :refer [get-field-value]]))

(defonce user-profile (r/atom nil))

(defonce show-login (r/atom false))

(defn auth-required?
  []
  (if-not (undefined? js/__authRequired)
    (boolean js/__authRequired)
    false))

(defn authenticated?
  []
  (not (nil? @user-profile)))

(defn set-user-profile!
  [profile]
  (reset! user-profile profile))

(defn show-login-form!
  []
  (reset! show-login true))

(defn hide-login-form!
  []
  (reset! show-login false))

(defn logout!
  []
  (reset! user-profile nil)
  (ajax/POST (->url "/logout")))