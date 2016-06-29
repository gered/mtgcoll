(ns mtgcoll.views.sente
  (:require
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.immutant :refer [sente-web-server-adapter]]))

(defonce sente-socket (atom {}))

(defn wrap-sente
  [handler uri]
  (fn [request]
    (let [uri-match? (.startsWith (str (:uri request)) uri)
          method     (:request-method request)]
      (cond
        (and uri-match? (= :get method))  ((:ajax-get-or-ws-handshake-fn @sente-socket) request)
        (and uri-match? (= :post method)) ((:ajax-post-fn @sente-socket) request)
        :else                             (handler request)))))

(defn init!
  []
  (reset! sente-socket
          (sente/make-channel-socket!
            sente-web-server-adapter
            {:user-id-fn        (fn [request] (get-in request [:params :client-id]))
             :handshake-data-fn (fn [request]
                                  {:user (get-in request [:session :user])})})))

(defn shutdown!
  []
  (reset! sente-socket {}))
