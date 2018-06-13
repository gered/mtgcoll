(ns mtgcoll.scrapers.prices
  (:require
    [views.sql.core :refer [hint-type]]
    [mtgcoll.views.core :refer [view-system]]
    [mtgcoll.models.cards :as cards]
    [mtgcoll.models.sets :as sets]
    [mtgcoll.scrapers.protocols :refer [scrape]]
    [mtgcoll.scrapers.registered :refer [price-scrapers]]))

(defn update-prices!
  ([source]
   (println "Updating" source "card prices.")
   (if-let [price-scraper (get price-scrapers source)]
     (do
       (doseq [{:keys [code gatherer_code] :as set} (sets/get-set-codes)]
         (println "Scraping prices for set:" code (if gatherer_code (str "(" gatherer_code ")") ""))
         (let [{:keys [source prices normalized-name? multiverse-id?]} (scrape price-scraper set)]
           (if (seq prices)
             (cards/update-prices! source prices {:normalized-name? normalized-name?
                                                  :multiverse-id?   multiverse-id?})
             (println "Could not obtain prices for set:" code (if gatherer_code (str "(" gatherer_code ")") ""))))))
     (println "No price scraper \"" source "\" found.")))
  ([]
   (println "Updating prices using all available scrapers.")
    (doseq [source (keys price-scrapers)]
      (update-prices! source))))
