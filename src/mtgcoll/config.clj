(ns mtgcoll.config
  (:refer-clojure :exclude [get])
  (:require
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [config.core :as config]
    [mount.core :as mount :refer [defstate]]))

(defstate app-config
  :start (let [config-file (get-in (mount/args) [:options :config])]
           (if (and config-file
                    (.exists (io/file config-file)))
             (do
               (log/info (str "Loading app config from " config-file))
               (config/load config-file {}))
             (throw (Exception. "No config file specified or does not exist.")))))

(defn get
  [& ks]
  (apply config/get app-config ks))