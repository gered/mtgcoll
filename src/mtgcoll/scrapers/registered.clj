(ns mtgcoll.scrapers.registered
  (:require
    mtgcoll.scrapers.prices.mtggoldfish
    mtgcoll.scrapers.prices.scryfall)
  (:import
    (mtgcoll.scrapers.prices.mtggoldfish MTGGoldFishPriceScraper)
    (mtgcoll.scrapers.prices.scryfall ScryfallPriceScraper)))

(def price-scrapers
  {:mtggoldfish (MTGGoldFishPriceScraper.)
   :scryfall (ScryfallPriceScraper.)})

