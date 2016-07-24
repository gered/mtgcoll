(ns mtgcoll.client.views
  (:require
    [reagent.core :as r]
    [taoensso.sente :as sente]
    [views.reagent.sente.client :as vr]
    [mtgcoll.client.auth :as auth]))

(defonce sente-socket (atom {}))

(defonce connected (r/atom false))

(defn connected?
  []
  (boolean @connected))

(defn chsk-exists?
  []
  (not (nil? (:chsk @sente-socket))))

(defn chsk-active?
  []
  (if-let [state (:state @sente-socket)]
    (:open? @state)))

(defn- clear-keepalive-interval!
  []
  (let [state (:state @sente-socket)
        state (if state @state)]
    (if (and (:open? state)
             (= :ws (:type state)))
      (.clearInterval js/window @(get-in @sente-socket [:chsk :kalive-timer_])))))

(defn sente-event-msg-handler
  [{:keys [event id client-id] :as ev}]
  (let [[ev-id ev-data] event]
    (cond
      (and (= :chsk/state ev-id)
           (:open? ev-data))
      (vr/on-open! @sente-socket ev)

      (= :chsk/handshake ev-id)
      (let [[_ _ handshake-data] ev-data
            {:keys [user]}       handshake-data]
        (reset! connected true)
        (auth/set-user-profile! user))

      (= :chsk/recv id)
      (when-not (vr/on-receive! @sente-socket ev)
        ; on-receive! returns true if the event was a views.reagent event and it
        ; handled it.
        ;
        ; you could put your code to handle your app's own events here
        ))))

(defn reconnect!
  []
  (clear-keepalive-interval!)
  (reset! connected false)
  (sente/chsk-reconnect! (:chsk @sente-socket)))

(defn init!
  []
  (if (chsk-exists?)
    (reconnect!)
    (reset! sente-socket (sente/make-channel-socket! "/chsk" {})))
  (sente/start-chsk-router! (:ch-recv @sente-socket) sente-event-msg-handler)
  (vr/init! @sente-socket))
