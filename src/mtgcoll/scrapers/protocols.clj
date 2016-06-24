(ns mtgcoll.scrapers.protocols)

(defprotocol PriceScraper
  (scrape [this set]))
