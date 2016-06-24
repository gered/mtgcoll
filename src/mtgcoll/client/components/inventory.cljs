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
  [card-id quality]
  (ajax/POST (->url "/collection/add")
             :params {:card-id card-id :quality quality}
             :on-error #(set-error! "Server error while adding card to inventory.")))

(defn on-remove-card
  [card-id quality]
  (ajax/POST (->url "/collection/remove")
             :params {:card-id card-id :quality quality}
             :on-error #(set-error! "Server error while adding card to inventory.")))

(defvc inventory-management
  [card-id]
  (let [inventory (view-cursor :owned-card card-id)
        inventory (group-by :quality @inventory)]
    [bs/Grid {:fluid true :class "inventory-container"}
     (map-indexed
       (fn [idx quality]
         (let [inventory (first (get inventory quality))
               quantity  (or (:quantity inventory) 0)]
           ^{:key idx}
           [bs/Row
            {:class (if (> quantity 0) "bg-warning")}
            [bs/Col {:sm 6 :class "text-right"}
             [bs/FormControl.Static
              (str (string/capitalize quality) ": ")]]
            [bs/Col {:sm 2}
             [bs/FormControl.Static
              [:strong quantity]]]
            [bs/Col {:sm 4}
             [bs/ButtonGroup {:justified true}
              [bs/ButtonGroup
               [bs/Button {:bsStyle "success" :on-click #(on-add-card card-id quality)}
                [bs/Glyphicon {:glyph "plus"}]]]
              [bs/ButtonGroup
               [bs/Button {:bsStyle "danger" :disabled (= 0 quantity) :on-click #(on-remove-card card-id quality)}
                [bs/Glyphicon {:glyph "minus"}]]]]]]))
       qualities)]))

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