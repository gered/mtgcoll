(ns image-assets
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.java.jdbc :as sql]
    [clj-http.client :as http]
    [config.core :as config]
    [mtgcoll.db :refer [db]]
    [mtgcoll.config :refer [config]])
  (:use
    mtgcoll.utils))

;; NOTE: The Gatherer site serves up images via the image handler as PNG's but
;;       the HTTP response incorrectly sets a "image/jpeg" content-type
;;       (which is probably only intended for the card images).
;;       The mana/set/symbol images are all actually PNG!

(defn ->gatherer-image-handler-url
  [size & {:keys [name rarity set]}]
  (-> "http://gatherer.wizards.com/Handlers/Image.ashx?type=symbol"
      (str "&size=" size)
      (str (if name (str "&name=" name)))
      (str (if rarity (str "&rarity=" rarity)))
      (str (if set (str "&set=" set)))))

(defn download-image-as-byte-array
  [url]
  (let [response (http/get url {:headers chrome-osx-request-headers
                                :as :byte-array})
        body     (:body response)]
    (if-not (empty? body)
      body)))

(defn save-bytes-to-file!
  [filename bytes]
  (let [file (io/file (str (config/get config :gatherer :image-save-path) filename))
        path (io/file (.getParent file))]
    (.mkdirs path)
    (with-open [w (io/output-stream (.getPath file))]
      (.write w bytes))))

(defn get-gatherer-set-codes
  []
  (sql/query db ["select code, gatherer_code from sets"]))

(defn download-set-image
  [size {:keys [code gatherer_code]}]
  (if-let [image-bytes (download-image-as-byte-array (->gatherer-image-handler-url size :rarity "C" :set code))]
    image-bytes
    (if-let [image-bytes (if gatherer_code
                           (download-image-as-byte-array (->gatherer-image-handler-url size :rarity "C" :set gatherer_code)))]
      image-bytes
      (download-image-as-byte-array (->gatherer-image-handler-url size :rarity "M" :set code)))))

(defn download-gatherer-set-images!
  [size]
  (doseq [code (get-gatherer-set-codes)]
    (println code)
    (if-let [image-bytes (download-set-image size code)]
      (save-bytes-to-file! (str "/sets/" size "/" (:code code) ".png") image-bytes)
      (println "Could not get image for:" code))))

#_(download-gatherer-set-images! "small")
#_(download-gatherer-set-images! "medium")

(defn download-symbol-image
  [size name]
  (download-image-as-byte-array (->gatherer-image-handler-url size :name name)))

(def mana-symbols
  ["C" "W" "U" "B" "R" "G" "S" "X" "Y" "Z" "WU" "WB" "UB" "UR" "BR" "BG" "RG" "RW" "GW" "GU" "2W" "2U" "2B" "2R" "2G" "P" "WP" "UP" "BP" "RP" "GP" "INFINITY" "H" "HW" "HU" "HB" "HR" "HG" "0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "10" "11" "12" "13" "14" "15" "16" "17" "18" "19" "20" "100" "1000000"])

(def gather-symbol-names
  {"S"  "snow"
   "HR" "HalfR"
   "T"  "tap"
   "Q"  "untap"})

(def other-symbols
  ["T" "Q" "CHAOS"])

(defn download-gatherer-symbol-image!
  [size type symbol-name]
  (when-let [image-bytes (download-symbol-image size (or (get gather-symbol-names symbol-name) symbol-name))]
    (save-bytes-to-file! (str "/" type "/" size "/" symbol-name ".png") image-bytes)
    true))

(defn download-gatherer-symbol-images!
  [size]
  ; mana symbols
  (doseq [symbol-name mana-symbols]
    (println symbol-name)
    (if-not (download-gatherer-symbol-image! size "mana" symbol-name)
      (println "Could not get image for:" symbol-name)))
  ; other symbols
  (doseq [symbol-name other-symbols]
    (println symbol-name)
    (if-not (download-gatherer-symbol-image! size "symbols" symbol-name)
      (println "Could not get image for:" symbol-name))))

#_(download-gatherer-symbol-images! "small")
#_(download-gatherer-symbol-images! "medium")
