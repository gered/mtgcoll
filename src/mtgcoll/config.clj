(ns mtgcoll.config
  (:require
    [config.core :as cfg]))

(defonce config (cfg/load (System/getenv "config")))
