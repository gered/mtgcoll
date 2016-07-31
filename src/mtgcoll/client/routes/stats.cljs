(ns mtgcoll.client.routes.stats
  (:require
    [reagent.core :as r]
    [cljsjs.chartjs]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.reagent.bootstrap :as bs]
    [webtools.reagent.components :refer [->keyed-comps]]
    [webtools.cljs.utils :refer [->url]]
    [mtgcoll.common :as c]
    [mtgcoll.client.auth :as auth]
    [mtgcoll.client.page :refer [set-active-breadcrumb!]]
    [mtgcoll.client.utils :refer [format-number format-currency get-field-value]]
    [mtgcoll.client.components.utils :refer [set-short-label]]
    [mtgcoll.client.components.cards :refer [card-link]]))

(defn widget-row
  [& components]
  [bs/Row {:class "widget-row"}
   (->keyed-comps components)])

(defn big-number-statistic
  [title number & [{:keys [width] :as options}]]
  [bs/Col {:sm (or width 12) :class "big-number-statistic"}
   [:div.title title]
   [:div.number (format-number number)]])

(defn big-currency-statistic
  [title number & [{:keys [width] :as options}]]
  [bs/Col {:sm (or width 12) :class "big-number-statistic"}
   [:div.title title]
   [:div.number (format-currency number)]])

(defn- render-vertical-chart-legend
  [chart]
  ;; HACK: usage of aget where '.-' notation *SHOULD* have worked
  ;;       (in practice, it was only not working for 'datasets' in production builds
  ;;       even though the js object clearly DID have this field).
  ;;       just changing them all to aget for consistency
  (let [data    (-> chart (aget "data"))
        labels  (-> data (aget "labels"))
        dataset (-> data (aget "datasets") first)]
    (r/render-to-string
      [:ul {:id (aget chart "id")}
       (map-indexed
         (fn [idx label]
           (let [bg-color (-> dataset (aget "backgroundColor") (aget idx))]
             ^{:key idx}
             [:li [:span {:style {:background-color bg-color}}]
              label]))
         labels)])))

(defn pie-chart
  [data & [{:keys [vertical-legend?] :as options}]]
  (r/create-class
    {:display-name "pie-chart"
     :component-did-mount
     (fn [this]
       (let [canvas  (aget (.-refs this) "canvas")
             legend  (aget (.-refs this) "legend")
             context (.getContext canvas "2d")
             options {:type    "pie"
                      :options (merge
                                 {:responsive          true
                                  :maintainAspectRatio true}
                                 (if vertical-legend?
                                   {:legend         false
                                    :legendCallback render-vertical-chart-legend})
                                 options)
                      :data    data}
             chart   (js/Chart. context (clj->js options))]
         (if legend (set! (.-innerHTML legend) (.generateLegend chart)))
         chart))

     :component-function
     (fn [data & [{:keys [vertical-legend?] :as options}]]
       [:div.chart-container
        (if vertical-legend?
          [:div.legend-left {:ref "legend"}])
        [:canvas.pie-chart {:ref "canvas"}]])}))

(defn- ->chart-data
  [data labels data-ks bg-colors]
  (assert (= (count labels) (count data-ks) (count bg-colors)))
  {:labels   (vec labels)
   :datasets [{:data            (mapv #(get data %) data-ks)
               :backgroundColor (vec bg-colors)}]})

(defvc widget-color-totals
  [online? list-id & [{:keys [width] :as options}]]
  (let [color-totals (view-cursor :stats/color-totals online? list-id (auth/get-username))]
    (if-not (vc/loading? color-totals)
      [bs/Col {:sm (or width 12) :class "widget"}
       [:div.title "Card Colors"]
       [pie-chart
        (->chart-data @color-totals
                      ["Black" "Blue" "Green" "Red" "White" "Colorless"]
                      [:black :blue :green :red :white :colorless]
                      ["#ccc2c0" "#aae1fa" "#9bd3ae" "#f9aa8f" "#fffcd5" "#dd99dd"])
        {:vertical-legend? true}]])))

(defvc widget-basic-type-totals
  [online? list-id & [{:keys [width] :as options}]]
  (let [basic-type-totals (view-cursor :stats/basic-type-totals online? list-id (auth/get-username))]
    (if-not (vc/loading? basic-type-totals)
      [bs/Col {:sm (or width 12) :class "widget"}
       [:div.title "Basic Card Types"]
       [pie-chart
        (->chart-data @basic-type-totals
                      ["Artifact" "Creature" "Enchantment" "Instant" "Land" "Planeswalker" "Tribal" "Sorcery"]
                      [:artifacts :creatures :enchantments :instants :lands :planeswalkers :tribals :sorcerys]
                      ["#4d4d4d" "#5da5da" "#faa43a" "#60bd68" "#f17cb0" "#b2912f" "#b276b2" "#decf3f"])
        {:vertical-legend? true}]])))

(defn table-statistics
  [title columns data & [{:keys [width] :as options}]]
  (let [columns (partition 2 columns)]
    [bs/Col {:sm (or width 12) :class "widget"}
     [:div.title title]
     [bs/Table {:striped true :condensed true :bordered true :hover true}
      [:thead
       [:tr
        (map-indexed
          (fn [idx [heading _]]
            ^{:key idx} [:th heading])
          columns)]]
      [:tbody
       (map-indexed
         (fn [idx row-data]
           ^{:key idx}
           [:tr
            (map-indexed
              (fn [idx [_ cell-fn]]
                ^{:key idx} [:td (cell-fn row-data)])
              columns)])
         data)]]]))

(defvc widget-most-owned-sets
  [online? list-id & [options]]
  (let [data (view-cursor :stats/most-owned-sets online? list-id (auth/get-username))]
    (if-not (vc/loading? data)
      [table-statistics
       "Cards Owned By Set"
       ["Set"         #(vec [:a {:href (->url "#/set/" (:set_code %))}
                             [:div [set-short-label (:set_code %)]]])
        "Cards Owned" :quantity]
       @data
       options])))

(defvc widget-most-copies-of-card
  [online? list-id & [options]]
  (let [data (view-cursor :stats/most-copies-of-card online? list-id (auth/get-username))]
    (if-not (vc/loading? data)
      [table-statistics
       "Most Copies Owned"
       ["Card"   #(vec [card-link (:id %) (:name %) :block-element? true])
        "Set"    #(vec [:a {:href (->url "#/set/" (:set_code %))}
                        [:div [set-short-label (:set_code %)]]])
        "Copies" :quantity]
       @data
       options])))

(defvc widget-most-nonland-copies-of-card
  [online? list-id & [options]]
  (let [data (view-cursor :stats/most-nonland-copies-of-card online? list-id (auth/get-username))]
    (if-not (vc/loading? data)
      [table-statistics
       "Most Non-land Copies Owned"
       ["Card"   #(vec [card-link (:id %) (:name %) :block-element? true])
        "Set"    #(vec [:a {:href (->url "#/set/" (:set_code %))}
                        [:div [set-short-label (:set_code %)]]])
        "Copies" :quantity]
       @data
       options])))

(defvc widget-most-valuable-cards
  [online? list-id pricing-source & [options]]
  (let [data (view-cursor :stats/most-valuable-cards online? pricing-source list-id (auth/get-username))]
    (if-not (vc/loading? data)
      [table-statistics
       "Most Valuable Cards"
       ["Card"  #(vec [card-link (:id %) (:name %) :block-element? true])
        "Set"   #(vec [:a {:href (->url "#/set/" (:set_code %))}
                       [:div [set-short-label (:set_code %)]]])
        "Price" #(format-currency (:price %))]
       @data
       options])))

(defvc widget-rarity-totals
  [online? list-id & [options]]
  (let [data (view-cursor :stats/card-rarity-totals online? list-id (auth/get-username))]
    (if-not (vc/loading? data)
      [table-statistics
       "Cards Owned By Rarity"
       ["Rarity" :rarity
        "Amount" :total]
       @data
       options])))

(defvc widget-agg-price-stats
  [online? list-id pricing-source & [{:keys [width] :as options}]]
  (let [agg-price-stats (view-cursor :stats/agg-price-stats online? pricing-source list-id (auth/get-username))
        min-price       (:min_price @agg-price-stats)
        max-price       (:max_price @agg-price-stats)
        avg-price       (:avg_price @agg-price-stats)
        median-price    (:median_price @agg-price-stats)]
    (when-not (vc/loading? agg-price-stats)
      [bs/Col {:sm (or width 12) :class "widget"}
       [big-currency-statistic "Lowest Card Value" min-price]
       [big-currency-statistic "Highest Card Value" max-price]
       [big-currency-statistic "Average Card Value" avg-price]
       [big-currency-statistic "Median Card Value" median-price]])))

(defvc widget-summary-stats
  [online? list-id pricing-source & [{:keys [width] :as options}]]
  (let [username                  (auth/get-username)
        agg-price-stats           (view-cursor :stats/agg-price-stats online? pricing-source list-id username)
        owned-total               (view-cursor :stats/owned-total online? list-id username)
        owned-foil-total          (view-cursor :stats/owned-foil-total online? list-id username)
        distinct-owned-total      (view-cursor :stats/distinct-owned-total online? list-id username)
        total-sets-owned-from     (view-cursor :stats/total-sets-owned-from online? list-id username)
        total-sets-owned-all-from (view-cursor :stats/total-sets-owned-all-from online? list-id username)
        total-price               (view-cursor :stats/total-price online? pricing-source list-id username)
        num-worth-over-1-dollar   (view-cursor :stats/num-cards-worth-over-1-dollar online? pricing-source list-id username)]
    [bs/Col {:sm (or width 12) :class "widget"}
     [widget-row [big-number-statistic "Owned" @owned-total]]
     [widget-row [big-number-statistic "Unique" @distinct-owned-total]]
     [widget-row [big-currency-statistic "Total Value" @total-price]]
     [widget-row [big-currency-statistic "Lowest Card Value" (:min_price @agg-price-stats)]]
     [widget-row [big-currency-statistic "Highest Card Value" (:max_price @agg-price-stats)]]
     [widget-row [big-currency-statistic "Average Card Value" (:avg_price @agg-price-stats)]]
     [widget-row [big-currency-statistic "Median Card Value" (:median_price @agg-price-stats)]]
     [widget-row [big-number-statistic "Cards Owned Over $1 Value" @num-worth-over-1-dollar]]
     [widget-row [big-number-statistic "Partial Sets" @total-sets-owned-from]]
     [widget-row [big-number-statistic "Complete Sets" @total-sets-owned-all-from]]
     [widget-row [big-number-statistic "Owned Foil Cards" @owned-foil-total]]]))


(defonce settings (r/atom {:online?        false
                           :pricing-source nil}))

(defvc stats-page
  []
  (let [pricing-sources (view-cursor :pricing-sources)
        online?         (:online? @settings)
        pricing-source  (:pricing-source @settings)
        list-id         c/owned-list-id]
    (set-active-breadcrumb! :stats)
    (if (and (not (vc/loading? pricing-sources))
             (nil? (:pricing-source @settings)))
      (swap! settings assoc :pricing-source (->> @pricing-sources first :source)))
    [:div.statistics-container
     [:div.header
      [bs/PageHeader "Collection Statistics"]
      [:div.settings
       [bs/Form {:inline true}
        [bs/FormGroup {:bsSize "large"}
         [bs/FormControl
          {:component-class "select"
           :value           (if online? "Online" "Paper")
           :on-change       #(swap! settings assoc :online? (= "Online" (get-field-value %)))}
          [:option "Paper"]
          [:option "Online"]]]
        [bs/FormGroup {:bsSize "large"}
         [bs/FormControl
          {:component-class "select"
           :value           (or (:pricing-source @settings) "")
           :on-change       #(swap! settings assoc :pricing-source (get-field-value %))}
          (map
            (fn [{:keys [source]}]
              ^{:key source} [:option {:value (or source "")} source])
            @pricing-sources)]]]]]
     [bs/Grid {:fluid true :class "statistics"}
      [widget-row
       [bs/Col {:sm 4}
        [widget-row [widget-summary-stats online? list-id pricing-source]]
        [widget-row [widget-rarity-totals online? list-id]]
        [widget-row [widget-most-owned-sets online? list-id]]]
       [bs/Col {:sm 8}
        [widget-row [widget-color-totals online? list-id]]
        [widget-row [widget-basic-type-totals online? list-id]]
        [widget-row [widget-most-valuable-cards online? list-id pricing-source {:width 10}]]
        [widget-row [widget-most-copies-of-card online? list-id {:width 10}]]
        [widget-row [widget-most-nonland-copies-of-card online? list-id {:width 10}]]]]]]))
