(ns import
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.java.jdbc :as sql]
    [cheshire.core :as json]
    [pantomime.mime :refer [mime-type-of]]
    [config.core :as config]
    [mtgcoll.config :refer [config]]
    [mtgcoll.db :refer [db]])
  (:use
    mtgcoll.utils)
  (:import (java.text SimpleDateFormat)
           (java.sql Timestamp)))

(defonce all-sets-json (atom nil))

(def mtg-json-path (config/get config :import :json-path))
(def mtg-card-images-path (str mtg-json-path "/AllSets"))

(defn load-json!
  []
  (with-open [rdr (io/reader (str mtg-json-path "/AllSets.json"))]
    (reset! all-sets-json (json/parse-stream rdr true)))
  (count (keys @all-sets-json)))

(defn get-card-image-file
  [set-code card-image-name]
  (let [image-filename (str mtg-card-images-path "/" set-code "/" (string/lower-case card-image-name) ".jpg")
        image-file     (io/file image-filename)]
    (if (.exists image-file)
      (with-open [rdr (io/input-stream image-filename)]
        (let [length (.length (io/file image-filename))
              buffer (byte-array length)]
          (.read rdr buffer, 0, length)
          {:bytes    buffer
           :mimetype (mime-type-of image-file)})))))

(defn parse-date
  [^String s]
  (if s
    (Timestamp. (.getTime (.parse (SimpleDateFormat. "yyyy-MM-dd") s)))))

(defn create-sets!
  []
  (doseq [[_ m] @all-sets-json]
    (try
      (sql/insert!
        db
        :sets
        {:code                  (:code m)
         :name                  (:name m)
         :gatherer_code         (:gathererCode m)
         :old_code              (:oldCode m)
         :magic_cards_info_code (:magicCardsInfoCode m)
         :release_date          (parse-date (:releaseDate m))
         :border                (:border m)
         :type                  (:type m)
         :block                 (:block m)
         :online_only           (boolean (:onlineOnly m))})
      (println (:code m) "-" (:name m))
      (catch Exception e
        (println e)
        (println (dissoc m :cards))
        (throw e)))))

(defn create-card-names!
  [db-con card-id names]
  (doseq [name names]
    (sql/insert!
      db-con
      :card_names
      {:card_id card-id
       :name    name})))

(defn create-card-variations!
  [db-con card-id multiverse-ids]
  (doseq [id multiverse-ids]
    (sql/insert!
      db-con
      :card_variations
      {:card_id         card-id
       :multiverseid    id
       :variant_card_id nil})))

(defn create-card-image!
  [db-con card-id image-name {:keys [bytes mimetype]}]
  (sql/insert!
    db-con
    :card_images
    {:card_id     card-id
     :image_name  image-name
     :image_bytes bytes
     :mimetype    mimetype}))

(defn create-cards-for-set!
  [set-code cards]
  (reduce
    (fn [cnt card]
      (let [card-id    (:id card)
            names      (:names card)
            variations (:variations card)
            image-name (:imageName card)
            image-file (get-card-image-file set-code image-name)]
        (try
          (sql/with-db-transaction
            [db-con db]
            (sql/insert!
              db-con
              :cards
              {:id                  card-id
               :set_code            set-code
               :layout              (:layout card)
               :name                (:name card)
               :normalized_name     (normalize-string (:name card))
               :mana_cost           (:manaCost card)
               :converted_mana_cost (:cmc card)
               :colors              (string/join "," (sort (:colors card)))
               :color_identity      (string/join "," (sort (:colorIdentity card)))
               :type                (:type card)
               :supertypes          (string/join "," (sort (:supertypes card)))
               :types               (string/join "," (sort (:types card)))
               :subtypes            (string/join "," (sort (:subtypes card)))
               :rarity              (:rarity card)
               :text                (:text card)
               :flavor              (:flavor card)
               :artist              (:artist card)
               :number              (:number card)
               :power               (:power card)
               :toughness           (:toughness card)
               :loyalty             (:loyalty card)
               :multiverseid        (:multiverseid card)
               :image_name          (:imageName card)
               :watermark           (:watermark card)
               :border              (:border card)
               :timeshifted         (boolean (:timeshifted card))
               :hand                (:hand card)
               :life                (:life card)
               :reserved            (boolean (:reserved card))
               :release_date        (:releaseDate card)
               :starter             (boolean (:starter card))})
            (create-card-names! db-con card-id names)
            (create-card-variations! db-con card-id variations)
            (create-card-image! db-con card-id image-name image-file))
          (catch Exception e
            (println e)
            (println card)
            (throw e))))
      (inc cnt))
    0
    cards))

(defn create-cards!
  []
  (doseq [[_ m] @all-sets-json]
    (let [num-created (create-cards-for-set! (:code m) (:cards m))]
      (println (:code m) "-" (:name m) "-" num-created "cards"))))

(defn fillin-card-variation-ids!
  []
  (sql/execute!
    db
    ["update card_variations
      set variant_card_id = (select c.id
                             from cards c
                             where c.multiverseid = card_variations.multiverseid)"]))

(defn load-to-sql!
  []
  (load-json!)
  (println "---- CREATING SETS ----")
  (create-sets!)
  (println "\n\n---- CREATING CARDS ----")
  (create-cards!)
  (println "\n\n---- FILLING IN CARD IDS FOR CARD VARIATIONS ----")
  (fillin-card-variation-ids!)
  (println "\n\n done!"))

#_(load-to-sql!)
