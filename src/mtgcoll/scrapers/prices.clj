(ns mtgcoll.scrapers.prices
  (:require
    [views.core :as views]
    [views.sql.core :refer [hint-type]]
    [mtgcoll.views.core :refer [view-system]]
    [mtgcoll.models.cards :as cards]
    [mtgcoll.models.sets :as sets]
    [mtgcoll.scrapers.protocols :refer [scrape]]
    [mtgcoll.scrapers.registered :refer [price-scrapers]]
    [mtgcoll.db :as db]))

(defn update-prices!
  ([source]
   (println "Updating" source "card prices.")
   (db/verify-connection)
   (if-let [price-scraper (get price-scrapers source)]
     (do
       (doseq [{:keys [code gatherer_code] :as set} (sets/get-set-codes)]
         (println "Scraping prices for set:" code (if gatherer_code (str "(" gatherer_code ")") ""))
         (let [{:keys [source prices normalized-name?]} (scrape price-scraper set)]
           (if prices
             (cards/update-prices! source prices {:normalized-name? normalized-name?})
             (println "Could not obtain prices for set:" code (if gatherer_code (str "(" gatherer_code ")") ""))))))
     (println "No price scraper \"" source "\" found.")))
  ([]
   (println "Updating prices using all available scrapers.")
    (doseq [source (keys price-scrapers)]
      (update-prices! source))))
