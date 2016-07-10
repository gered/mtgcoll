(ns mtgcoll.client.routes.cards
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.cljs.utils :refer [->url pprint-json]]
    [webtools.reagent.bootstrap :as bs]
    [webtools.reagent.components :refer [->keyed-comps raw-html]]
    [mtgcoll.client.components.cards :refer [card-image card-link]]
    [mtgcoll.client.components.utils :refer [symbol-image set-heading set-label symboled-markup]]
    [mtgcoll.client.components.inventory :refer [inventory]]
    [mtgcoll.client.utils :refer [format-date format-datetime format-currency]]))

(defn detail
  [& components]
  [bs/Row {:class "details-row"}
   (->keyed-comps components)])

(defn details-label
  [& components]
  [bs/Col {:sm 4 :class "details-label"}
   (->keyed-comps components)])

(defn details-value
  [& components]
  [bs/Col {:sm 8 :class "details-value"}
   (->keyed-comps components)])

(defvc card-details
  [id]
  (let [card       (view-cursor :full-card-info id)
        variations (view-cursor :card-variations id)
        pricing    (view-cursor :card-pricing id)
        printings  (view-cursor :card-printings (:card_name @card))]
    (cond
      (and (not (vc/loading? card))
           (nil? @card))
      [:div "Card " id " not found."]

      (vc/loading? card)
      [:div "Loading ..."]

      :else
      [:div
       [bs/PageHeader
        {:class "card-title"}
        (:card_name @card) " "
        [:a {:href (->url "#/set/" (:set_code @card))}
         [:small [set-heading (:set_code @card) (:set_name @card)]]]
        [:div.pull-right
         [inventory id
          {:num-owned (:owned_count @card)
           :button-size "large"
           :button-style (if (> (:owned_count @card) 0) "primary")}]]]
       [bs/Grid
        {:class "card" :fluid true}
        [bs/Col {:sm 6 :class "card-image"}
         [bs/Row
          [bs/Col [card-image id]]]
         (if (seq @variations)
           [bs/Row
            [bs/Col {:class "card-variations"}
             [:strong "Variations: "
              (->keyed-comps
                (as-> @variations x
                      (conj x {:id id :card_name (:card_name @card)})
                      (sort-by :id x)
                      (map-indexed
                        (fn [idx {:keys [card_name] :as variation}]
                          (if (not= id (:id variation))
                            [card-link (:id variation) card_name :link-text [:strong (inc idx)] :popover-placement "top"]
                            [:span.text-muted (inc idx)]))
                        x)
                      (interpose [:span.text-muted ", "] x)))]]])]
        [bs/Col {:sm 6 :class "card-details"}
         [bs/Grid {:class "card-details-table" :fluid true}
          (if-not (string/blank? (:mana_cost @card))
            [detail
             [details-label "Mana Cost"]
             [details-value
              [:span
               [symboled-markup (:mana_cost @card)]
               [raw-html :span (str " &mdash; " (:converted_mana_cost @card))]]]])
          (if-not (string/blank? (:colors @card))
            [detail
             [details-label "Colors"]
             [details-value (as-> (:colors @card) x
                                  (string/split x #",")
                                  (map string/trim x)
                                  (string/join ", " x))]])
          (if-not (string/blank? (:color_identity @card))
            [detail
             [details-label "Color Identity"]
             [details-value (map-indexed
                              (fn [idx color-symbol]
                                ^{:key idx} [symbol-image color-symbol])
                              (string/split (:color_identity @card) #","))]])
          [detail
           [details-label "Type"]
           [details-value (:type @card)]]
          [detail
           [details-label "Card Text"]
           [details-value
            [:div.card-text
             [symboled-markup (:text @card) :italicize? true :split-paragraphs? true]]]]
          (if-not (string/blank? (:flavor @card))
            [detail
             [details-label "Flavor Text"]
             [details-value
              [:div.flavor-text
               [symboled-markup (:flavor @card) :split-paragraphs? true :wrap-with :em]]]])
          (if (or (:power @card)
                  (:toughness @card))
            [detail
             [details-label "P/T"]
             [details-value (:power @card) " / " (:toughness @card)]])
          (if (:loyalty @card)
            [detail
             [details-label "Loyalty"]
             [details-value (:loyalty @card)]])
          [detail
           [details-label "Rarity"]
           [details-value (:rarity @card)]]
          [detail
           [details-label "Border"]
           [details-value (string/capitalize (or (:border @card) (:set_border @card)))]]
          [detail
           [details-label "Layout"]
           [details-value (string/capitalize (:layout @card))]]
          (if (:number @card)
            [detail
             [details-label "Number"]
             [details-value (:number @card)]])
          (cond
            (:release_date @card)
            [detail
             [details-label "Release Date"]
             [details-value (:release_date @card)]]

            (:set_release_date @card)
            [detail
             [details-label "Release Date"]
             [details-value (format-date (:set_release_date @card))]])
          (if (:starter @card)
            [detail
             [details-label "Starter Box"]
             [details-value "Yes"]])
          (if (:online_only @card)
            [detail
             [details-label "Online Only"]
             [details-value "Yes"]])
          [detail
           [details-label "Artist"]
           [details-value (:artist @card)]]
          (if (:multiverseid @card)
            [detail
             [details-label "Multiverse ID"]
             [details-value [:a {:href (str "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" (:multiverseid @card))}
                             (:multiverseid @card)]]])]
         (if (seq @pricing)
           [bs/Grid {:class "card-details-table" :fluid true}
            [:h3 "Pricing"]
            [bs/Table {:striped true :bordered true :condensed true :hover true}
             [:thead
              [:tr
               [:th "Source"]
               [:th "Paper"]
               [:th "Online"]
               [:th "Last Updated"]]]
             [:tbody
              (->> @pricing
                   (group-by :source)
                   (map-indexed
                     (fn [idx [source prices]]
                       (let [online-price (->> prices (filter :online) (first) :price)
                             paper-price  (->> prices (remove :online) (first) :price)
                             last-updated (->> prices (sort-by :last_updated_at) (first) :last_updated_at)]
                         ^{:key idx}
                         [:tr
                          [:td source]
                          [:td (format-currency paper-price true)]
                          [:td (format-currency online-price true)]
                          [:td (format-datetime last-updated)]]))))]]])
         (if (and (seq @printings)
                  (> (count @printings) 1))
           [bs/Grid {:class "card-details-table" :fluid true}
            [:h3 "All Printings"]
            [:div.card-printings-table
             {:class (if (> (count @printings) 10)
                       "scroll")}
             [bs/Table {:striped true :bordered true :condensed true :hover true}
              [:thead
               [:tr
                [:th "Set"]
                [:th "Release Date"]
                ]]
              [:tbody
               (->> @printings
                    (sort-by :set_release_date >)
                    (map
                      (fn [{:keys [id card_name set_code set_name set_release_date online_only]}]
                        (let [current-card? (= id (:id @card))
                              link-text     [set-label set_code set_name]]
                          ^{:key id}
                          [:tr {:class (if current-card? "info")}
                           [:td (if (not current-card?)
                                  [card-link id card_name
                                   :block-element? true
                                   :link-text link-text
                                   :popover-placement "left"]
                                  link-text)]
                           [:td (str (format-date set_release_date))]])))
                    (doall))]]]])]]])))

