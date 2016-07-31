(ns mtgcoll.client.routes.sets
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.reagent.bootstrap :as bs]
    [webtools.cljs.utils :refer [->url]]
    [mtgcoll.common :as c]
    [mtgcoll.client.page :refer [set-active-breadcrumb!]]
    [mtgcoll.client.components.cards :refer [card-list-table ->card-list-pager]]
    [mtgcoll.client.components.utils :refer [set-image set-label set-heading th-sortable]]
    [mtgcoll.client.components.search :as s]
    [mtgcoll.client.utils :refer [format-date]]))

(defvc sets-list
  []
  (let [sort-settings (r/atom
                        {:sort-by    :name
                         :ascending? true})]
    (fn []
      (let [sets (view-cursor :sets-list)]
        (set-active-breadcrumb! :sets)
        [:div
         [bs/PageHeader "Sets"]
         (if (vc/loading? sets)
           [:div "Loading ..."]
           [bs/Table
            {:bordered true :striped true :condensed true :hover true}
            [:thead
             [:tr
              [th-sortable sort-settings :name "Name"]
              [th-sortable sort-settings :release_date "Release Date"]
              [th-sortable sort-settings :type "Type"]
              [th-sortable sort-settings :block "Block"]
              [th-sortable sort-settings :online_only "Online"]
              [th-sortable sort-settings :border "Border"]
              [th-sortable sort-settings :card_count "Cards"]
              [th-sortable sort-settings :owned_count "Owned (Unique)"]]]
            [:tbody
             (->> @sets
                  (sort-by (:sort-by @sort-settings)
                           (if (:ascending? @sort-settings) < >))
                  (map
                    (fn [{:keys [code owned_count] :as set}]
                      (let [owned? (> owned_count 0)]
                        ^{:key code}
                        [:tr
                         (if owned? {:class "warning"})
                         [:td [:a {:href (->url "#/set/" code)} [:div [set-label code (:name set)]]]]
                         [:td (format-date (:release_date set))]
                         [:td (:type set)]
                         [:td (:block set)]
                         [:td (if (:online_only set) "Yes" "No")]
                         [:td (string/capitalize (:border set))]
                         [:td (:card_count set)]
                         [:td (if (> owned_count 0) owned_count)]]))))]])]))))

(defonce set-cards-search-filters
  (r/atom (s/->search-filters)))

(defn set-cards-list
  [set-code]
  (let [fixed-filters         [{:field :set-code :value set-code :comparison :=}]
        active-search-filters (r/cursor set-cards-search-filters [:active-filters])
        pager                 (r/cursor set-cards-search-filters [:pager])
        list-id               c/owned-list-id]
    (s/apply-search-filters! set-cards-search-filters fixed-filters)
    (fn []
      [:div.set-cards-list
       [s/search-filter-selector set-cards-search-filters {:fixed-active-filters fixed-filters}]
       [card-list-table list-id @active-search-filters pager]])))

(defvc set-details
  [set-code]
  (let [set (view-cursor :set-info set-code)]
    (set-active-breadcrumb! :sets)
    (cond
      (and (not (vc/loading? set))
           (nil? @set))
      [:div "Set " set-code " not found."]

      (vc/loading? set)
      [:div "Loading ..."]

      :else
      [:div
       [bs/PageHeader (:name @set)]
       [bs/Grid {:fluid true :class "set-details"}
        [bs/Row
         [bs/Col {:sm 2 :class "text-center"} [set-image (:code @set) :size "large"]]
         [bs/Col {:sm 10}
          [bs/Row
           [bs/Col {:sm 2 :class "details-label"} "Code"]
           [bs/Col {:sm 2} (:code @set)]
           [bs/Col {:sm 2 :class "details-label"} "Release Date"]
           [bs/Col {:sm 2} (format-date (:release_date @set))]]
          [bs/Row
           [bs/Col {:sm 2 :class "details-label"} "Border"]
           [bs/Col {:sm 2} (string/capitalize (:border @set))]
           [bs/Col {:sm 2 :class "details-label"} "Online"]
           [bs/Col {:sm 2} (if (:online_only @set) "Yes" "No")]]
          [bs/Row
           [bs/Col {:sm 2 :class "details-label"} "Type"]
           [bs/Col {:sm 2} (:type @set)]
           [bs/Col {:sm 2 :class "details-label"} "Block"]
           [bs/Col {:sm 2} (:block @set)]]
          [bs/Row
           [bs/Col {:sm 2 :class "details-label"} "Cards"]
           [bs/Col {:sm 2} (or (:card_count @set) 0)]
           [bs/Col {:sm 2 :class "details-label"} "Owned (Unique)"]
           [bs/Col {:sm 2} (or (:owned_count @set) 0)]]]]]
       [set-cards-list set-code]])))