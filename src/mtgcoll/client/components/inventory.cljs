(ns mtgcoll.client.components.inventory
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.reagent.bootstrap :as bs]
    [webtools.cljs.ajax :as ajax]
    [webtools.cljs.utils :refer [->url]]
    [mtgcoll.client.page :refer [set-error!]]))

(def qualities
  ["online" "near mint" "lightly played" "moderately played" "heavily played" "damaged"])

(defn on-add-card
  [card-id quality foil?]
  (ajax/POST (->url "/collection/add")
             :params {:card-id card-id :quality quality :foil foil?}
             :on-error #(set-error! "Server error while adding card to inventory.")))

(defn on-remove-card
  [card-id quality foil?]
  (ajax/POST (->url "/collection/remove")
             :params {:card-id card-id :quality quality :foil foil?}
             :on-error #(set-error! "Server error while adding card to inventory.")))

(defvc inventory-management
  [card-id]
  (let [inventory (view-cursor :owned-card card-id)
        inventory (group-by :quality @inventory)]
    [bs/Grid {:fluid true :class "inventory-container"}
     [bs/Table
      {:condensed true :hover true :bordered true}
      [:thead
       [:tr
        [:th ""]
        [:th {:col-span 2} [:span.text-center "Normal"]]
        [:th {:col-span 2} [:span.text-center "Foil"]]]]
      [:tbody
       (map-indexed
         (fn [idx quality]
           (let [inventory         (get inventory quality)
                 quantities        (group-by :foil inventory)
                 foil-quantity     (or (:quantity (first (get quantities true))) 0)
                 non-foil-quantity (or (:quantity (first (get quantities false))) 0)]
             ^{:key idx}
             [:tr
              {:class (if (or (> foil-quantity 0)
                              (> non-foil-quantity 0))
                        "bg-warning")}
              [:td.quality-label.col-sm-4
               [:span.text-right
                [bs/FormControl.Static
                 (str (string/capitalize quality) ": ")]]]
              ;; non-foil
              [:td.quantity.col-sm-1
               [bs/FormControl.Static
                [:strong non-foil-quantity]]]
              [:td.col-sm-3
               [bs/ButtonGroup {:justified true}
                [bs/ButtonGroup
                 [bs/Button {:bsStyle "success" :on-click #(on-add-card card-id quality false)}
                  [bs/Glyphicon {:glyph "plus"}]]]
                [bs/ButtonGroup
                 [bs/Button {:bsStyle "danger" :disabled (= 0 non-foil-quantity) :on-click #(on-remove-card card-id quality false)}
                  [bs/Glyphicon {:glyph "minus"}]]]]]
              ;; foil
              [:td.quantity.col-sm-1
               [bs/FormControl.Static
                [:strong foil-quantity]]]
              [:td.col-sm-3
               [bs/ButtonGroup {:justified true}
                [bs/ButtonGroup
                 [bs/Button {:bsStyle "success" :on-click #(on-add-card card-id quality true)}
                  [bs/Glyphicon {:glyph "plus"}]]]
                [bs/ButtonGroup
                 [bs/Button {:bsStyle "danger" :disabled (= 0 foil-quantity) :on-click #(on-remove-card card-id quality true)}
                  [bs/Glyphicon {:glyph "minus"}]]]]]]))
         qualities)]]]))

(defn inventory
  [card-id & [{:keys [num-owned owned? button-size button-style] :as opts}]]
  [bs/OverlayTrigger {:placement "bottom"
                      :trigger "click"
                      :root-close true
                      :overlay (r/as-component
                                 [bs/Popover {:class "inventory" :title "Card Inventory"}
                                  [inventory-management card-id]])}
   [bs/Button
    (merge
      {:block true}
      (if button-size {:bsSize button-size})
      (if button-style {:bsStyle button-style}))
    (if (and num-owned
             (> (or num-owned 0) 0))
      [:span "Owned: " [:strong (or num-owned 0)]]
      (if owned? "Owned" "Not Owned"))
    " "
    [:span.caret]]])