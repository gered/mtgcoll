(ns mtgcoll.cli
  (:require
    [clojure.string :as string]
    [clojure.tools.cli :as cli]))

(def ^:private cli-options
  [["-c" "--config EDN-CONFIG-FILE"
    "EDN configuration file to use. If ommitted, assumes one is located in the current working directory."
    "-h" "--help"]])

(def ^:private actions
  [["web" "(default) Runs the web application"]
   ["setup-db" "Initializes database"]
   ["load-json" "Loads mtgjson card/set data from specified json file"]
   ["scrape-prices" "Runs all price scrapers, updating card pricing"]
   ["scrape-set-images" "Downloads set images from gatherer"]])

(defn- ->actions-summary
  []
  (->> actions
       (map (fn [[action desc]] (str "  " action "\t\t" desc)))
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

(defn- exit
  [status msg]
  (println msg)
  (System/exit status))

(defn parse-cli-args
  [args]
  (let [{:keys [options arguments errors summary] :as parsed-cli} (cli/parse-opts args cli-options)
        arguments     (or arguments ["web"])
        action        (first arguments)
        arguments     (rest arguments)
        valid-action? (some #{action} (map first actions))]
    (cond
      (:help options)     (exit 0 (->usage-string summary))
      (not valid-action?) (exit 1 (->usage-string summary))
      errors              (exit 1 (->error-msg errors)))
    {:options   options
     :action    (keyword action)
     :arguments arguments}))
