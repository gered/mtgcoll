(ns mtgcoll.models.mtgjson
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.java.jdbc :as sql]
    [cheshire.core :as json]
    [pantomime.mime :refer [mime-type-of]]
    [mtgcoll.db :refer [db]])
  (:use
    mtgcoll.utils)
  (:import
    (java.text SimpleDateFormat)
    (java.sql Timestamp)))

(defn load-json
  [json-file]
  (with-open [rdr (io/reader json-file)]
    (json/parse-stream rdr true)))

(defn parse-date
  [^String s]
  (if s
    (Timestamp. (.getTime (.parse (SimpleDateFormat. "yyyy-MM-dd") s)))))

(defn create-sets!
  [json-data]
  (doseq [[_ m] json-data]
    (try
      (sql/insert!
        @db
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
      (println "Added set" (:code m) "-" (:name m))
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

(defn create-cards-for-set!
  [set-code cards]
  (reduce
    (fn [cnt card]
      (let [card-id    (:id card)
            names      (:names card)
            variations (:variations card)]
        (try
          (sql/with-db-transaction
            [db-con @db]
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
            (create-card-variations! db-con card-id variations))
          (catch Exception e
            (println e)
            (println card)
            (throw e))))
      (inc cnt))
    0
    cards))

(defn create-cards!
  [json-data]
  (doseq [[_ m] json-data]
    (let [num-created (create-cards-for-set! (:code m) (:cards m))]
      (println "Added" num-created "cards for set" (:code m) "-" (:name m)))))

(defn fillin-card-variation-ids!
  []
  (println "Filling in card IDs for card variations")
  (sql/execute!
    @db
    ["update card_variations
      set variant_card_id = (select c.id
                             from cards c
                             where c.multiverseid = card_variations.multiverseid)"]))

(defn load-mtgjson-data!
  [json-file]
  (println "Loading MTG JSON data")
  (if-not (file-exists? json-file)
    (println "Error loading MTG JSON data: file not found.")
    (let [json-data (load-json json-file)]
      (create-sets! json-data)
      (create-cards! json-data)
      (fillin-card-variation-ids!)
      (println "Done!")
      true)))
