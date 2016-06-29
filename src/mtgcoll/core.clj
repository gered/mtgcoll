(ns mtgcoll.core
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [compojure.core :refer [routes GET POST]]
    [compojure.route :as route]
    [immutant.web :as immutant]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring-ttl-session.core :refer [ttl-memory-store]]
    [taoensso.sente.server-adapters.immutant :refer [sente-web-server-adapter]]

    [mtgcoll.cli :refer [parse-cli-args]]
    [mtgcoll.config :as config]
    [mtgcoll.db :as db]
    [mtgcoll.models.mtgjson :refer [load-mtgjson-data!]]
    [mtgcoll.scrapers.image-assets :refer [download-gatherer-set-images! download-gatherer-symbol-images!]]
    [mtgcoll.scrapers.prices :refer [update-prices!]]
    [mtgcoll.views.core :as views]
    [mtgcoll.views.sente :as sente]
    [mtgcoll.routes.main-page :refer [main-page-routes]]
    [mtgcoll.routes.images :refer [image-routes]]
    [mtgcoll.routes.collection :refer [collection-routes]]
    [mtgcoll.routes.auth :refer [auth-routes]]))

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

(def handler
  (-> (routes
        auth-routes
        collection-routes
        image-routes
        main-page-routes
        (route/resources "/")
        (route/not-found "not found"))
      (wrap-restful-format :formats [:json-kw])
      (sente/wrap-sente "/chsk")
      (wrap-defaults (assoc-in site-defaults [:session :store] (ttl-memory-store (* 60 30))))))

(defn start-server!
  []
  (init)
  (let [options (merge
                  {:port         8080
                   :host         "localhost"
                   :path         "/"
                   :virtual-host nil
                   :dispatch?    true}
                  (config/get :web))]
    (if (config/get :dev?)
      ; why the fuck would anyone assume that if you are running in "development mode" you automatically
      ; want a new browser window/tab to be opened each time the web server is started? AND not provide an
      ; option to disable this annoying-as-fuck behaviour.
      ; i mean, jesus christ, it's obviously _not_ possible that i might already have a browser open that i
      ; might prefer to keep reusing ... !
      (with-redefs [clojure.java.browse/browse-url (fn [_])]
        (immutant/run-dmc #'handler options))
      (immutant/run #'handler options))))

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
