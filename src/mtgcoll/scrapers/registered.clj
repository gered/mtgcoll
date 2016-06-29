(ns mtgcoll.scrapers.registered
  (:require
    mtgcoll.scrapers.prices.mtggoldfish)
  (:import
    (mtgcoll.scrapers.prices.mtggoldfish MTGGoldFishPriceScraper)))

(def price-scrapers
  {:mtggoldfish (MTGGoldFishPriceScraper.)})

