(ns mtgcoll.cli
  (:require
    [clojure.string :as string]
    [clojure.tools.cli :as cli])
  (:use
    mtgcoll.utils))

(def ^:private cli-options
  [["-c" "--config EDN-CONFIG-FILE"
    "EDN configuration file to use. If ommitted, assumes one is located in the current working directory."
    "-h" "--help"]])

(def ^:private actions
  [["web" "(default) Runs the web application"]
   ["setup-db" "Initializes database"]
   ["load-json" "Loads mtgjson card/set data from specified json file"]
   ["scrape-prices" "Runs all price scrapers, updating card pricing"]
   ["scrape-images" "Downloads set/mana/symbol images from Gatherer"]])

(defn- ->actions-summary
  []
  (->> actions
       (map (fn [[action desc]] (str "  " (pad-string 20 action) " " desc)))
       (string/join \newline)))

(defn- ->usage-string
  [options-summary]
  (->> ["MTG Web Collection"
        ""
        "Usage: mtgcoll [options] [action] [arguments]"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        (->actions-summary)]
       (string/join \newline)))

(defn- ->error-msg
  [errors]
  (str "Invalid options/arguments.\n"
       (string/join \newline errors)))

(defn show-help!
  [{:keys [summary] :as parsed-cli}]
  (println (->usage-string summary)))

(defn show-error!
  [{:keys [errors] :as parsed-cli}]
  (println (->error-msg errors)))

(defn parse-args
  [args]
  (let [{:keys [options arguments errors summary] :as parsed-cli} (cli/parse-opts args cli-options)
        arguments     (or (seq arguments) ["web"])
        action        (first arguments)
        arguments     (rest arguments)
        valid-action? (boolean (some #{action} (map first actions)))]
    {:options       options
     :action        (keyword action)
     :arguments     arguments
     :summary       summary
     :errors        errors
     :valid-action? valid-action?}))
