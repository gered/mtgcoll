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

(defn get-username
  []
  (:username @user-profile))

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
  [on-success]
  (ajax/POST (->url "/logout")
             :on-success (fn [_]
                           (reset! user-profile nil)
                           (on-success))))
