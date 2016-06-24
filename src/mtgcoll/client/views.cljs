(ns mtgcoll.client.views
  (:require
    [taoensso.sente :as sente]
    [views.reagent.sente.client :as vr]))

(defonce sente-socket (atom {}))

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

(defn reconnect!
  []
  (clear-keepalive-interval!)
  (sente/chsk-reconnect! (:chsk @sente-socket)))

(defn init!
  []
  (if (chsk-exists?)
    (reconnect!)
    (reset! sente-socket (sente/make-channel-socket! "/chsk" {})))
  (vr/init! @sente-socket {:use-default-sente-router? true}))
