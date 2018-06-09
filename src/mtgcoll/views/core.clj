(ns mtgcoll.views.core
  (:require
    [clojure.tools.logging :as log]
    [honeysql.format :as fmt]
    [mount.core :refer [defstate]]
    [views.core :as views]
    [views.reagent.sente.server :as vr]
    [views.sql.view :refer [view]]
    [mtgcoll.db :refer [db]]
    [mtgcoll.auth :as auth]
    [mtgcoll.views.sente :refer [sente-socket]]
    [mtgcoll.views.functions.cards :as cards]
    [mtgcoll.views.functions.sets :as sets]
    [mtgcoll.views.functions.collection :as collection]
    [mtgcoll.views.functions.lists :as lists]
    [mtgcoll.views.functions.prices :as prices]
    [mtgcoll.views.functions.statistics :as statistics]))

(defn get-db
  [_]
  db)

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

   (view :card-inventory get-db #'collection/card-inventory)
   (view :total-card-inventory get-db #'collection/total-card-inventory)

   (view :card-pricing get-db #'prices/card-pricing)
   (view :pricing-sources get-db #'prices/pricing-sources)

   (view :list-info get-db #'lists/list-info {:result-set-fn first})
   (view :list-settings get-db #'lists/list-settings {:result-set-fn first})
   (view :lists-list get-db #'lists/lists-list)
   (view :lists-basic-list get-db #'lists/lists-basic-list)

   (view :stats/owned-total get-db #'statistics/owned-total {:row-fn :total :result-set-fn first})
   (view :stats/owned-foil-total get-db #'statistics/owned-foil-total {:row-fn :total :result-set-fn first})
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

(defn view-auth-fn
  [{:keys [view-id parameters] :as view-sig} subscriber-key context]
  (let [request  context
        username (get-in request [:session :user :username])]
    (if-not (auth/using-authorization?)
      true

      (case view-id
        ; views where the user-id parameter is always last
        (:stats/owned-total :stats/owned-foil-total :stats/distinct-owned-total :stats/color-totals
         :stats/basic-type-totals :stats/most-common-types :stats/total-sets-owned-from
         :stats/total-sets-owned-all-from :stats/most-owned-sets :stats/most-copies-of-card
         :stats/most-nonland-copies-of-card :stats/total-price :stats/agg-price-stats :stats/most-valuable-cards
         :stats/num-cards-worth-over-1-dollar :stats/card-rarity-totals :full-card-info
         :list-info :list-settings :lists-list :lists-basic-list :card-inventory :total-card-inventory)
        (= username (last parameters))

        ; views where the user-id parameter is second
        (:cards :count-of-cards)
        (= username (second parameters))

        ; assume otherwise that the view is not one that requires an auth check (no user-id parameter)
        true))))

(defn view-on-unauth-fn
  [{:keys [view-id parameters] :as view-sig} subscriber-key context]
  (let [request      context
        user-profile (get-in request [:session :user])]
    (log/warn "Unauthorized view subscription attempt: " view-id ", " parameters " - user profile: " user-profile)))

(defstate ^{:on-reload :noop} view-system
  :start (let [vs (atom nil)]
           (vr/init! vs sente-socket
                     {:views                     views
                      :use-default-sente-router? true
                      :auth-fn                   view-auth-fn
                      :on-unauth-fn              view-on-unauth-fn})
           vs)
  :stop  (views/shutdown! view-system))

(defmethod fmt/fn-handler "ilike"
  [_ col qstr]
  (str (fmt/to-sql col) " ilike " (fmt/to-sql qstr)))
