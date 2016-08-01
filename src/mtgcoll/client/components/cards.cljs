(ns mtgcoll.client.components.cards
  (:require
    [reagent.core :as r]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.cljs.utils :refer [->url]]
    [webtools.reagent.bootstrap :as bs]
    [webtools.reagent.components :refer [raw-html]]
    [mtgcoll.common :refer [max-search-results]]
    [mtgcoll.client.auth :as auth]
    [mtgcoll.client.components.utils :refer [set-short-label symboled-markup th-sortable]]
    [mtgcoll.client.components.inventory :refer [inventory]]
    [mtgcoll.client.utils :refer [format-currency]]))

(defn card-image
  [card-id & {:keys [width height]}]
  [:img (merge
          {:src (->url "/images/cards/" card-id)}
          (if (and width height)
            {:width width
             :height height}))])

(defn card-link
  [card-id card-name & {:keys [block-element? no-popover? link-text popover-placement]}]
  (let [placement (or popover-placement "right")
        link-text (or link-text [:strong card-name])
        link      [:a {:href (->url "#/card/" card-id)}
                   (if block-element?
                     [:div link-text]
                     link-text)]]
    (if no-popover?
      link
      [bs/OverlayTrigger
       {:placement placement
        :animation false
        :overlay   (r/as-component
                     [bs/Popover {:class "card-image"}
                      [card-image card-id :width 240 :height 320]])}
       link])))

(defn ->card-list-pager
  []
  {:page 0 :page-size 20})

(defn max-pages
  [{:keys [page-size] :as pager}]
  (int (js/Math.floor (/ max-search-results page-size))))

(defn too-many-results?
  [num-results]
  (> num-results max-search-results))

(defvc card-list-table
  [list-id filters pager & [{:keys [no-owned-highlight?] :as options}]]
  (let [sort-settings (r/atom
                        {:sort-by    :name
                         :ascending? true})]
    (fn [list-id filters pager & [{:keys [no-owned-highlight?] :as options}]]
      (let [cards      (view-cursor :cards list-id (auth/get-username) filters (:sort-by @sort-settings) (:ascending? @sort-settings) (:page @pager) (:page-size @pager))
            card-count (view-cursor :count-of-cards list-id (auth/get-username) filters)
            num-pages  (min (js/Math.ceil (/ @card-count (:page-size @pager)))
                            (max-pages @pager))]
        (if (vc/loading? cards)
          [:div "Loading ..."]
          (if (empty? @cards)
            [bs/Alert {:bsStyle "warning"} "No cards found."]
            [:div.cards-list
             [bs/Table {:striped true :bordered true :condensed true :hover true}
              [:thead
               [:tr
                [th-sortable sort-settings :name "Name"]
                [th-sortable sort-settings :set "Set"]
                [th-sortable sort-settings :mana-cost "Mana Cost"]
                [th-sortable sort-settings :type "Type"]
                [th-sortable sort-settings :rarity "Rarity"]
                [th-sortable sort-settings :paper-price "Paper"]
                [th-sortable sort-settings :online-price "Online"]
                [th-sortable sort-settings :inventory "Inventory"]]]
              [:tbody
               (map
                 (fn [{:keys [id name set_code set_name mana_cost type power toughness rarity paper_price online_price quantity] :as card}]
                   (let [quantity (or quantity 0)
                         owned?   (> quantity 0)]
                     ^{:key id}
                     [:tr {:class (if (and (not no-owned-highlight?) owned?) "warning")}
                      [:td [card-link id name :block-element? true]]
                      [:td [:a {:href (->url "#/set/" set_code)} [set-short-label set_code set_name]]]
                      [:td [symboled-markup mana_cost]]
                      [:td (str type
                                (if (or power toughness)
                                  (str " (" power "/" toughness ")")))]
                      [:td rarity]
                      [:td (format-currency paper_price true)]
                      [:td (format-currency online_price true)]
                      [:td [inventory id list-id
                            {:num-owned quantity
                             :button-size "xsmall"
                             :button-style (if owned? "primary")}]]]))
                 @cards)]]
             [:div.paging
              [:div.text-center
               [bs/Pagination
               {:prev (r/as-component [raw-html :span "&lsaquo; Previous"])
                :next (r/as-component [raw-html :span "Next &rsaquo;"])
                :ellipsis true
                :boundaryLinks true
                :items num-pages
                :max-buttons 7
                :active-page (inc (:page @pager))
                :on-select #(swap! pager assoc-in [:page] (dec %))}]]
              [:div.text-center
               (if (too-many-results? @card-count)
                 [bs/FormControl.Static "Showing first " max-search-results " of " @card-count " total results."]
                 [bs/FormControl.Static "Showing " @card-count " total " (if (= 1 @card-count) "result." "results.")])]]]))))))
