(ns mtgcoll.views.core
  (:require
    [views.reagent.sente.server :as vr]
    [views.core :as views]
    [views.sql.view :refer [view]]
    [honeysql.format :as fmt]
    [mtgcoll.db :refer [db]]
    [mtgcoll.views.sente :refer [sente-socket]]
    [mtgcoll.views.functions.cards :as cards]
    [mtgcoll.views.functions.sets :as sets]
    [mtgcoll.views.functions.collection :as collection]
    [mtgcoll.views.functions.prices :as prices]
    [mtgcoll.views.functions.statistics :as statistics]))

(defonce view-system (atom {}))

(defn get-db
  [_]
  @db)

(def views
  [(view :card-info get-db #'cards/card-info {:result-set-fn first})
   (view :full-card-info get-db #'cards/full-card-info [:cards :collection :card_prices] {:result-set-fn first})
   (view :card-names get-db #'cards/card-names)
   (view :card-variations get-db #'cards/card-variations)
   (view :card-printings get-db #'cards/card-printings)
   (view :cards get-db #'cards/cards [:cards :collection :card_prices])
   (view :count-of-cards get-db #'cards/count-of-cards {:result-set-fn #(->> % first :count)})

   (view :set-info get-db #'sets/set-info {:result-set-fn first})
   (view :simple-sets-list get-db #'sets/simple-sets-list)
   (view :sets-list get-db #'sets/sets-list)

   (view :owned-card get-db #'collection/owned-card)
   (view :total-owned-of-card get-db #'collection/total-owned-of-card)

   (view :card-pricing get-db #'prices/card-pricing)
   (view :pricing-sources get-db #'prices/pricing-sources)

   (view :stats/owned-total get-db #'statistics/owned-total {:row-fn :sum :result-set-fn first})
   (view :stats/distinct-owned-total get-db #'statistics/distinct-owned-total {:row-fn :count :result-set-fn first})
   (view :stats/color-totals get-db #'statistics/color-totals {:result-set-fn first})
   (view :stats/basic-type-totals get-db #'statistics/basic-type-totals {:result-set-fn first})
   (view :stats/most-common-types get-db #'statistics/most-common-types)
   (view :stats/total-sets-owned-from get-db #'statistics/total-sets-owned-from {:row-fn :count :result-set-fn first})
   (view :stats/total-sets-owned-all-from get-db #'statistics/total-sets-owned-all-from {:row-fn :count :result-set-fn first})
   (view :stats/most-owned-sets get-db #'statistics/most-owned-sets)
   (view :stats/most-copies-of-card get-db #'statistics/most-copies-of-card)
   (view :stats/most-nonland-copies-of-card get-db #'statistics/most-nonland-copies-of-card)
   (view :stats/total-price get-db #'statistics/total-price {:row-fn :total :result-set-fn first})
   (view :stats/agg-price-stats get-db #'statistics/agg-price-stats {:result-set-fn first})
   (view :stats/most-valuable-cards get-db #'statistics/most-valuable-cards)
   (view :stats/num-cards-worth-over-1-dollar get-db #'statistics/num-cards-worth-over-1-dollar {:row-fn :count :result-set-fn first})
   (view :stats/card-rarity-totals get-db #'statistics/card-rarity-totals)])

#_(views/add-views! view-system views)

(defn init!
  []
  (vr/init! view-system @sente-socket
            {:views                     views
             :use-default-sente-router? true}))

(defn shutdown!
  []
  (views/shutdown! view-system))

(defmethod fmt/fn-handler "ilike"
  [_ col qstr]
  (str (fmt/to-sql col) " ilike " (fmt/to-sql qstr)))
