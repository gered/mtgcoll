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

(def views
  [(view :card-info db #'cards/card-info {:result-set-fn first})
   (view :full-card-info db #'cards/full-card-info [:cards :collection :card_prices] {:result-set-fn first})
   (view :card-names db #'cards/card-names)
   (view :card-variations db #'cards/card-variations)
   (view :card-printings db #'cards/card-printings)
   (view :cards db #'cards/cards [:cards :collection :card_prices])
   (view :count-of-cards db #'cards/count-of-cards {:result-set-fn #(->> % first :count)})

   (view :set-info db #'sets/set-info {:result-set-fn first})
   (view :simple-sets-list db #'sets/simple-sets-list)
   (view :sets-list db #'sets/sets-list)

   (view :owned-card db #'collection/owned-card)
   (view :total-owned-of-card db #'collection/total-owned-of-card)

   (view :card-pricing db #'prices/card-pricing)
   (view :pricing-sources db #'prices/pricing-sources)

   (view :stats/owned-total db #'statistics/owned-total {:row-fn :sum :result-set-fn first})
   (view :stats/distinct-owned-total db #'statistics/distinct-owned-total {:row-fn :count :result-set-fn first})
   (view :stats/color-totals db #'statistics/color-totals {:result-set-fn first})
   (view :stats/basic-type-totals db #'statistics/basic-type-totals {:result-set-fn first})
   (view :stats/most-common-types db #'statistics/most-common-types)
   (view :stats/total-sets-owned-from db #'statistics/total-sets-owned-from {:row-fn :count :result-set-fn first})
   (view :stats/total-sets-owned-all-from db #'statistics/total-sets-owned-all-from {:row-fn :count :result-set-fn first})
   (view :stats/most-owned-sets db #'statistics/most-owned-sets)
   (view :stats/most-copies-of-card db #'statistics/most-copies-of-card)
   (view :stats/most-nonland-copies-of-card db #'statistics/most-nonland-copies-of-card)
   (view :stats/total-price db #'statistics/total-price {:row-fn :total :result-set-fn first})
   (view :stats/agg-price-stats db #'statistics/agg-price-stats {:result-set-fn first})
   (view :stats/most-valuable-cards db #'statistics/most-valuable-cards)
   (view :stats/num-cards-worth-over-1-dollar db #'statistics/num-cards-worth-over-1-dollar {:row-fn :count :result-set-fn first})
   (view :stats/card-rarity-totals db #'statistics/card-rarity-totals)])

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
