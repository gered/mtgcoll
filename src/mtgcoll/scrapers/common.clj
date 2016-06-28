(ns mtgcoll.scrapers.common
  (:require
    [views.core :as views]
    [views.sql.core :refer [hint-type]]
    [mtgcoll.views.core :refer [view-system]]
    [mtgcoll.models.cards :as cards]
    [mtgcoll.models.sets :as sets]
    [mtgcoll.scrapers.protocols :refer [scrape]]
    [mtgcoll.scrapers.list :refer [price-scrapers]]
    [mtgcoll.db :as db]))

(defn update-prices!
  ([source]
   (println "Updating" source "card prices.")
   (db/verify-connection)
   (if-let [price-scraper (get price-scrapers source)]
     (do
       (doseq [set (sets/get-set-codes)]
         (println "Scraping prices for set:" set)
         (let [{:keys [source prices normalized-name?]} (scrape price-scraper set)]
           (if prices
             (cards/update-prices! source prices {:normalized-name? normalized-name?})
             (println "Could not obtain prices for set:" set))))
       (views/put-hints! view-system [(views/hint nil #{:card_prices} hint-type)]))
     (println "No price scraper \"" source "\" found.")))
  ([]
   (println "Updating prices using all available scrapers.")
    (doseq [source (keys price-scrapers)]
      (update-prices! source))))
