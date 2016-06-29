(ns mtgcoll.client.routes.collection
  (:require
    [reagent.core :as r]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.reagent.bootstrap :as bs]
    [webtools.reagent.components :refer [->keyed-comps]]
    [webtools.cljs.utils :refer [pprint-json]]
    [mtgcoll.client.page :refer [set-active-breadcrumb!]]
    [mtgcoll.client.components.cards :refer [card-list-table ->card-list-pager]]
    [mtgcoll.client.components.search :as s]
    [mtgcoll.client.utils :refer [get-field-value valid-float? valid-integer?]]))

(defonce all-cards-search-filters
  (r/atom (s/->search-filters)))

(defn all-cards-list
  []
  (let [active-search-filters (r/cursor all-cards-search-filters [:active-filters])
        pager                 (r/cursor all-cards-search-filters [:pager])]
    (fn []
      (set-active-breadcrumb! :all)
      [:div
       [bs/PageHeader "All Cards"]
       [s/search-filter-selector all-cards-search-filters]
       [card-list-table @active-search-filters pager]])))

;;;

(defonce owned-cards-search-filters
  (r/atom (s/->search-filters)))

(defn owned-cards-list
  []
  (let [fixed-filters         [{:field :owned? :value true :comparison :=}]
        active-search-filters (r/cursor owned-cards-search-filters [:active-filters])
        pager                 (r/cursor owned-cards-search-filters [:pager])]
    (s/apply-search-filters! owned-cards-search-filters fixed-filters)
    (fn []
      (set-active-breadcrumb! :owned)
      [:div
       [bs/PageHeader "Owned Cards"]
       [s/search-filter-selector owned-cards-search-filters {:fixed-active-filters fixed-filters}]
       [card-list-table @active-search-filters pager {:no-owned-highlight? true}]])))
