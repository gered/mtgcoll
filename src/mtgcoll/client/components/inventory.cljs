(ns mtgcoll.client.components.inventory
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.reagent.bootstrap :as bs]
    [webtools.cljs.ajax :as ajax]
    [webtools.cljs.utils :refer [->url]]
    [mtgcoll.common :as c]
    [mtgcoll.client.auth :as auth]
    [mtgcoll.client.page :refer [set-error!]]))

(def qualities
  ["online" "near mint" "lightly played" "moderately played" "heavily played" "damaged"])

(defn on-add-card
  [card-id quality foil? list-id]
  (ajax/POST (->url "/collection/add")
             :params {:card-id card-id :quality quality :foil foil? :list-id list-id}
             :on-error #(set-error! "Server error while adding card to inventory.")))

(defn on-remove-card
  [card-id quality foil? list-id]
  (ajax/POST (->url "/collection/remove")
             :params {:card-id card-id :quality quality :foil foil? :list-id list-id}
             :on-error #(set-error! "Server error while adding card to inventory.")))

(defn can-modify-inventory?
  []
  (or (not (auth/auth-required?))
      (auth/authenticated?)))

(defvc inventory-management
  [card-id list-id]
  (let [inventory      (view-cursor :card-inventory card-id list-id (auth/get-username))
        inventory      (group-by :quality @inventory)
        colspan        (if (can-modify-inventory?) 2 1)
        quantity-class (if (can-modify-inventory?)
                         "quantity col-sm-1"
                         "quantity col-sm-4")]
    [bs/Grid {:fluid true :class "inventory-container"}
     [bs/Table
      {:condensed true :hover true :bordered true}
      [:thead
       [:tr
        [:th ""]
        [:th.text-center {:col-span colspan} "Normal"]
        [:th.text-center {:col-span colspan} "Foil"]]]
      [:tbody
       (doall
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
                [:td {:class quantity-class}
                 [bs/FormControl.Static
                  (if (> non-foil-quantity 0)
                    [:strong non-foil-quantity]
                    [:span.text-muted 0])]]
                (if (can-modify-inventory?)
                  [:td.col-sm-3
                   [bs/ButtonGroup {:justified true}
                    [bs/ButtonGroup
                     [bs/Button {:bsStyle "success" :on-click #(on-add-card card-id quality false list-id)}
                      [bs/Glyphicon {:glyph "plus"}]]]
                    [bs/ButtonGroup
                     [bs/Button {:bsStyle "danger" :disabled (= 0 non-foil-quantity) :on-click #(on-remove-card card-id quality false list-id)}
                      [bs/Glyphicon {:glyph "minus"}]]]]])
                ;; foil
                [:td {:class quantity-class}
                 [bs/FormControl.Static
                  (if (> foil-quantity 0)
                    [:strong foil-quantity]
                    [:span.text-muted 0])]]
                (if (can-modify-inventory?)
                  [:td.col-sm-3
                   [bs/ButtonGroup {:justified true}
                    [bs/ButtonGroup
                     [bs/Button {:bsStyle "success" :on-click #(on-add-card card-id quality true list-id)}
                      [bs/Glyphicon {:glyph "plus"}]]]
                    [bs/ButtonGroup
                     [bs/Button {:bsStyle "danger" :disabled (= 0 foil-quantity) :on-click #(on-remove-card card-id quality true list-id)}
                      [bs/Glyphicon {:glyph "minus"}]]]]])]))
           qualities))]]]))

(defn inventory
  [card-id list-id & [{:keys [num-owned owned? button-size button-style] :as opts}]]
  [bs/OverlayTrigger {:placement "bottom"
                      :trigger "click"
                      :root-close true
                      :overlay (r/as-component
                                 [bs/Popover {:class "inventory" :title "Inventory"}
                                  [inventory-management card-id list-id]])}
   (let [owned-list? (= c/owned-list-id list-id)]
     [bs/Button
      (merge
        {:block true}
        (if button-size {:bsSize button-size})
        (if button-style {:bsStyle button-style}))
      (if (and num-owned
               (> (or num-owned 0) 0))
        (if owned-list?
          [:span "Owned: " [:strong (or num-owned 0)]]
          [:span [:strong (or num-owned 0)]])
        (if owned-list?
          (if owned? "Owned" "Not Owned")
          (if owned? "Yes" "None")))
      " "
      [:span.caret]])])