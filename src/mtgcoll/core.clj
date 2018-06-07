(ns mtgcoll.core
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [clojure.tools.nrepl.server :as nrepl-server]
    [compojure.core :refer [routes GET POST]]
    [compojure.route :as route]
    [immutant.web :as immutant]
    [mount.core :as mount :refer [defstate]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring-ttl-session.core :refer [ttl-memory-store]]
    [taoensso.sente.server-adapters.immutant :refer [sente-web-server-adapter]]
    [mtgcoll.cli :as cli]
    [mtgcoll.config :as config]
    [mtgcoll.db :as db]
    [mtgcoll.models.mtgjson :refer [load-mtgjson-data!]]
    [mtgcoll.scrapers.image-assets :refer [download-gatherer-set-images! download-gatherer-symbol-images!]]
    [mtgcoll.scrapers.prices :refer [update-prices!]]
    [mtgcoll.views.sente :as sente]
    [mtgcoll.routes.main-page :refer [main-page-routes]]
    [mtgcoll.routes.images :refer [image-routes]]
    [mtgcoll.routes.collection :refer [collection-routes]]
    [mtgcoll.routes.lists :refer [list-routes]]
    [mtgcoll.routes.auth :refer [auth-routes]]))

(def handler
  (-> (routes
        auth-routes
        collection-routes
        list-routes
        image-routes
        main-page-routes
        (route/resources "/")
        (route/not-found "not found"))
      (wrap-restful-format :formats [:json-kw])
      (sente/wrap-sente "/chsk")
      (wrap-defaults (assoc-in site-defaults [:session :store] (ttl-memory-store (* 60 30))))))

(defstate ^{:on-reload :noop} http-server
  :start (let [options (merge
                         {:host         "localhost"
                          :port         8080
                          :path         "/"
                          :virtual-host nil
                          :dispatch?    nil}
                         (config/get :http))]
           (log/info "Starting HTTP server: " options)
           (immutant/run (if (config/get :dev?)
                           (wrap-reload #'handler)
                           #'handler)
                         options))
  :stop (do
          (log/info "Stopping HTTP server")
          (immutant/stop http-server)))

(defstate ^{:on-reload :noop} repl-server
  :start (when-let [nrepl-config (config/get :nrepl)]
           (let [port (or (:port nrepl-config) 4000)]
             (log/info "Starting nREPL server on port: " port)
             (nrepl-server/start-server :port port)))
  :stop (when repl-server
          (log/info "Stopping nREPL server")
          (nrepl-server/stop-server repl-server)))

(defn -main
  [& args]
  (let [{:keys [options action arguments errors valid-action?] :as args} (cli/parse-args args)]
    (cond
      (or (:help options)
          (not valid-action?))
      (cli/show-help! args)

      errors
      (do
        (cli/show-error! errors)
        (System/exit 1))

      :else
      (case action
        :web
        (do
          (mount/start-with-args args)
          (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable mount/stop)))

        :setup-db
        (do
          (mount/start-with-args args #'config/app-config, #'db/db)
          (db/initialize-database!))

        :load-json
        (do
          (mount/start-with-args args #'config/app-config, #'db/db)
          (load-mtgjson-data! (first arguments)))

        :scrape-prices
        (do
          (mount/start-with-args args #'config/app-config, #'db/db)
          (if (seq arguments)
            (update-prices! (first arguments))
            (update-prices!)))

        :scrape-images
        (do
          (mount/start-with-args args #'config/app-config, #'db/db)
          (download-gatherer-set-images!)
          (download-gatherer-symbol-images!))))))
