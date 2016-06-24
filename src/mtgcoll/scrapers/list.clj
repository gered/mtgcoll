(ns mtgcoll.scrapers.list
  (:require
    mtgcoll.scrapers.mtggoldfish)
  (:import
    (mtgcoll.scrapers.mtggoldfish MTGGoldFishPriceScraper)))

(def price-scrapers
  {:mtggoldfish (MTGGoldFishPriceScraper.)})

