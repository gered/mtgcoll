(ns mtgcoll.scrapers.image-assets
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :as sql]
    [mtgcoll.db :refer [db]]
    [mtgcoll.config :as config])
  (:use
    mtgcoll.utils))

;; NOTE: The Gatherer site serves up images via the image handler as PNG's but
;;       the HTTP response incorrectly sets a "image/jpeg" content-type
;;       (which is probably only intended for the card images).
;;       The mana/set/symbol images are all actually PNG!

(defn- ->gatherer-image-handler-url
  [size & {:keys [name rarity set]}]
  (-> "http://gatherer.wizards.com/Handlers/Image.ashx?type=symbol"
      (str "&size=" size)
      (str (if name (str "&name=" name)))
      (str (if rarity (str "&rarity=" rarity)))
      (str (if set (str "&set=" set)))))

(defn- save-bytes-to-file!
  [filename bytes]
  (let [file (io/file (str (config/get :other-images-path) filename))
        path (io/file (.getParent file))]
    (.mkdirs path)
    (with-open [w (io/output-stream (.getPath file))]
      (.write w bytes))))

(defn- get-gatherer-set-codes
  []
  (sql/query @db ["select code, gatherer_code from sets order by code"]))

(defn- download-set-image
  [size {:keys [code gatherer_code]}]
  (if-let [image-bytes (download-as-byte-array (->gatherer-image-handler-url size :rarity "C" :set code))]
    image-bytes
    (if-let [image-bytes (if gatherer_code
                           (download-as-byte-array (->gatherer-image-handler-url size :rarity "C" :set gatherer_code)))]
      image-bytes
      ;; last attempt if all else fails -- use "M" rarity code. some set images are only available this way ...
      (download-as-byte-array (->gatherer-image-handler-url size :rarity "M" :set code)))))

(def ^:private sizes
  ["small" "medium" "large"])

(defn download-gatherer-set-images!
  []
  (println "Downloading set images from Gatherer")
  (doseq [{:keys [code gatherer_code] :as set} (get-gatherer-set-codes)]
    (println "Getting images for set:" code (if gatherer_code (str "(" gatherer_code ")") ""))
    (doseq [size sizes]
      (if-let [image-bytes (download-set-image size set)]
        (save-bytes-to-file! (str "/sets/" size "/" code ".png") image-bytes)
        (println "Unable to download" size "image for:" set)))))

;;;;

(defn- download-symbol-image
  [size name]
  (download-as-byte-array (->gatherer-image-handler-url size :name name)))

(def ^:private mana-symbols
  ["C" "W" "U" "B" "R" "G" "S" "X" "Y" "Z" "WU" "WB" "UB" "UR" "BR" "BG" "RG" "RW" "GW" "GU" "2W" "2U" "2B" "2R" "2G" "P" "WP" "UP" "BP" "RP" "GP" "INFINITY" "H" "HW" "HU" "HB" "HR" "HG" "0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "10" "11" "12" "13" "14" "15" "16" "17" "18" "19" "20" "100" "1000000"])

(def ^:private gather-symbol-names
  {"S"  "snow"
   "HR" "HalfR"
   "T"  "tap"
   "Q"  "untap"})

(def ^:private other-symbols
  ["T" "Q" "CHAOS"])

(defn- download-gatherer-symbol-image!
  [size type symbol-name]
  (when-let [image-bytes (download-symbol-image size (or (get gather-symbol-names symbol-name) symbol-name))]
    (save-bytes-to-file! (str "/" type "/" size "/" symbol-name ".png") image-bytes)
    true))

(defn download-gatherer-symbol-images!
  []
  ;; note: intentionally using "symbols" as type arg to download-gatherer-symbol-image!
  ;;       as it's just simpler to have both sets of images saved into the same "symbols" directory
  (println "Downloading mana symbol images from Gatherer")
  (doseq [symbol-name mana-symbols]
    (println "Getting images for mana symbol:" symbol-name)
    (doseq [size sizes]
      (if-not (download-gatherer-symbol-image! size "symbols" symbol-name)
        (println "Unable to download" size "image for mana symbol:" symbol-name))))
  ; other symbols
  (doseq [symbol-name other-symbols]
    (println "Getting images for symbol:" symbol-name)
    (doseq [size sizes]
      (if-not (download-gatherer-symbol-image! size "symbols" symbol-name)
        (println "Unable to download" size "image for other symbol:" symbol-name)))))
