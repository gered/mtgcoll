(ns mtgcoll.config
  (:refer-clojure :exclude [get])
  (:require
    [config.core :as cfg]))

(defonce app-config (atom {}))

(defn load!
  [config-file]
  (reset! app-config (cfg/load (or config-file (System/getenv "config")))))

(defn get
  [& ks]
  (apply cfg/get @app-config ks))