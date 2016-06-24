(ns mtgcoll.scrapers.common
  (:require
    [clojure.java.jdbc :as sql]
    [views.core :as views]
    [views.sql.core :refer [hint-type]]
    [mtgcoll.db :refer [db]]
    [mtgcoll.views.core :refer [view-system]]
    [mtgcoll.models.cards :as cards]
    [mtgcoll.models.sets :as sets]
    [mtgcoll.scrapers.protocols :refer [scrape]]
    [mtgcoll.scrapers.list :refer [price-scrapers]]))

(defn update-prices!
  [source]
  (if-let [price-scraper (get price-scrapers source)]
    (do
      (println "updating prices using" source "scraper.")
      (doseq [set (sets/get-set-codes)]
        (println "scraping prices for set:" set)
        (let [{:keys [source prices normalized-name?]} (scrape price-scraper set)]
          (if prices
            (cards/update-prices! source prices {:normalized-name? normalized-name?})
            (println "could not obtain prices for set:" set))))
      ; manually add a hint indicating the card_prices table was updated so all
      ; relevant views refresh. we intentionally do NOT update prices using vexec!
      ; as price updates are done via (potentially) thousands of inserts/updates
      ; which, if we used vexec!, would result in thousands of unnecessary view
      ; refreshes being triggered.
      (views/put-hints! view-system [(views/hint nil #{:card_prices} hint-type)]))))
