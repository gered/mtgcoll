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

    [mtgcoll.cli :refer [parse-cli-args]]
    [mtgcoll.config :as config]
    [mtgcoll.db :as db]
    [mtgcoll.models.mtgjson :refer [load-mtgjson-data!]]
    [mtgcoll.scrapers.image-assets :refer [download-gatherer-set-images! download-gatherer-symbol-images!]]
    [mtgcoll.scrapers.common :refer [update-prices!]]
    [mtgcoll.views.core :as views]
    [mtgcoll.views.sente :as sente]
    [mtgcoll.routes.main-page :refer [main-page-routes]]
    [mtgcoll.routes.images :refer [image-routes]]
    [mtgcoll.routes.collection :refer [collection-routes]]))

(defn init
  []
  (log/info "Starting up web application ...")

  (when (config/get :dev?)
    (log/info "Running in development environment."))

  (db/setup-config!)
  (db/verify-connection)
  (sente/init!)
  (views/init!)

  (log/info "Application init finished."))

(defn shutdown
  []
  (log/info "Shutting down ...")

  (views/shutdown!)
  (sente/shutdown!)

  (log/info "Application stopped."))

(defn wrap-env-middleware
  [handler]
  (if (config/get :dev?)
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

(defn start-server!
  []
  (init)
  (immutant/run
    #'handler
    (merge
      {:port         8080
       :host         "localhost"
       :path         "/"
       :virtual-host nil
       :dispatch?    true}
      (config/get :web))))

(defn stop-server!
  []
  (if (immutant/stop)
    (shutdown)))

(defn -main
  [& args]
  (let [{:keys [options action arguments]} (parse-cli-args args)]
    (config/load! (:config options))
    (db/setup-config!)
    (db/verify-connection)
    (case action
      :web
      (do
        (start-server!)
        (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop-server!)))

      :setup-db
      (db/initialize-database!)

      :load-json
      (load-mtgjson-data! (first arguments))

      :scrape-prices
      (if (seq arguments)
        (update-prices! (first arguments))
        (update-prices!))

      :scrape-images
      (do
        (download-gatherer-set-images!)
        (download-gatherer-symbol-images!)))))
