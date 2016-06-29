(ns mtgcoll.scrapers.prices.mtggoldfish
  (:require
    [clojure.string :as string]
    [clj-http.client :as http]
    [net.cgrand.enlive-html :as en]
    [mtgcoll.scrapers.protocols :refer [PriceScraper]])
  (:use
    mtgcoll.utils))

(defrecord MTGGoldFishPriceScraper []
  PriceScraper
  (scrape [_ {:keys [code gatherer_code]}]
    (let [result (http/get
                   (str "http://www.mtggoldfish.com/index/" code)
                   {:headers          chrome-osx-request-headers
                    :throw-exceptions false})
          result (if (and (= 404 (:status result))
                          gatherer_code)
                   (http/get
                     (str "http://www.mtggoldfish.com/index/" gatherer_code)
                     {:headers          chrome-osx-request-headers
                      :throw-exceptions false})
                   result)]
      (if (= 200 (:status result))
        (let [parsed-html   (en/html-resource (string->stream (:body result)))
              price-table   (en/select parsed-html [:div.index-price-table :table :tbody :tr])
              full-set-name (-> parsed-html (en/select [:div.index-price-header-index-name]) en/texts first string/trim)]
          (as-> price-table x
                (map
                  (fn [tr]
                    (let [cells         (en/select tr [:td])
                          card-link-tag (-> cells (en/select [:td.card :a]) first)
                          card-url      (get-in card-link-tag [:attrs :href])
                          online?       (.endsWith (str card-url) "#online")]
                      {:card-name         (en/text card-link-tag)
                       :online?           online?
                       :set-code          code
                       :original-set-code (-> cells (nth 1) en/text)
                       :rarity            (-> cells (nth 2) en/text)
                       :price             (-> cells (nth 3) en/text string/trim)}))
                  x)
                (map
                  (fn [{:keys [card-name] :as card-info}]
                    (let [[card-name number] (rest (re-matches #"^(.*?)(?:\ \((\d*)\))?$" card-name))]
                      (assoc card-info :card-name card-name
                                       :number number)))
                  x)
                (map
                  (fn [{:keys [price] :as card-info}]
                    (assoc card-info :price (parse-currency-string price)))
                  x)
                (reduce
                  (fn [coll {:keys [^String card-name price] :as card-info}]
                    (if (.contains card-name " // ")
                      (let [[left right] (string/split card-name #" // ")
                            card-info    (assoc card-info :price (/ price 2)
                                                          :split? true)]
                        (-> coll
                            (conj (assoc card-info :card-name left))
                            (conj (assoc card-info :card-name right))))
                      (conj coll card-info)))
                  []
                  x)
                (assoc
                  {:set-code code
                   :set-name full-set-name
                   :source :mtggoldfish
                   :normalized-name? true}
                  :prices x)))))))
