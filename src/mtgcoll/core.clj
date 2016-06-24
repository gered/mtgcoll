(ns mtgcoll.core
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [compojure.core :refer [routes GET POST]]
    [compojure.route :as route]
    [immutant.web :as immutant]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring.middleware.reload :refer [wrap-reload]]
    [taoensso.sente.server-adapters.immutant :refer [sente-web-server-adapter]]
    [views.reagent.sente.server :as vr]

    [config.core :as config]
    [mtgcoll.config :refer [config]]
    [mtgcoll.db :as db]
    [mtgcoll.views.core :as views]
    [mtgcoll.views.sente :as sente]
    [mtgcoll.routes.main-page :refer [main-page-routes]]
    [mtgcoll.routes.images :refer [image-routes]]
    [mtgcoll.routes.collection :refer [collection-routes]]))

(defn init
  []
  (log/info "Starting up ...")

  (when (config/get config :dev?)
    (log/info "Running in development environment."))

  (db/verify-connection)
  (sente/init!)
  (views/init!)

  (log/info "Application init finished."))

(defn shutdown
  []
  (log/info "Shutting down ...")
  (views/shutdown!)
  (sente/init!))

(defn wrap-env-middleware
  [handler]
  (if (config/get config :dev?)
    (-> handler (wrap-reload))
    handler))

(def handler
  (-> (routes
        collection-routes
        image-routes
        main-page-routes
        (route/resources "/")
        (route/not-found "not found"))
      (wrap-env-middleware)
      (wrap-restful-format :formats [:json-kw])
      (sente/wrap-sente "/chsk")
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))

(defn run-server
  []
  (init)
  (immutant/run #'handler {:port 8080}))

(defn -main
  [& args]
  (run-server))
