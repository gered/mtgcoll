(ns mtgcoll.scrapers.prices.scryfall
  (:require
    [clojure.string :as string]
    [cheshire.core :as json]
    [clj-http.client :as http]
    [mtgcoll.scrapers.protocols :refer [PriceScraper]]
    [mtgcoll.utils :as u]))

(def ^:private http-options
  {:headers          u/chrome-osx-request-headers
   :throw-exceptions false})

(defn- get-set-list
  [{:keys [code]}]
  (loop [cards []
         page  1]
    (let [url    (str "https://api.scryfall.com/cards/search?order=cmc&q=e:" code "+unique:prints" (if (> page 1) (str "&page=" page)))
          result (http/get url http-options)]
      (Thread/sleep 200)
      (if (= 200 (:status result))
        (let [result (json/parse-string (:body result) true)]
          (if (:has_more result)
            (recur (:data result) (inc page))
            (concat cards (:data result))))))))

(defrecord ScryfallPriceScraper []
  PriceScraper
  (scrape [_ {:keys [code] :as set}]
    (let [cards (get-set-list set)]
      (as-> cards x
            (map
              (fn [{:keys [name digital set rarity foil usd eur lang multiverse_ids collector_number]}]
                {:card-name    name
                 :online?      digital
                 :set-code     code
                 :rarity       rarity
                 :price        (u/parse-currency-string usd)
                 :number       collector_number
                 :multiverseid (first multiverse_ids)})
              x)
            (remove
              #(or (nil? (:price %))
                   (nil? (:multiverseid %)))
              x)
            (reduce
              (fn [coll {:keys [^String card-name price] :as card}]
                (if (.contains card-name " // ")
                  (let [[left right] (string/split card-name #" // ")
                        card (assoc card :price (/ price 2)
                                         :split? true)]
                    (-> coll
                        (conj (assoc card :card-name left))
                        (conj (assoc card :card-name right))))
                  (conj coll card)))
              []
              x)
            (assoc
              {:set-code       code
               :source         :scryfall
               :multiverse-id? true}
              :prices x)))))
