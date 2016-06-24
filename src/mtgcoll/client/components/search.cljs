(ns mtgcoll.client.components.search
  (:require
    [reagent.core :as r]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.reagent.bootstrap :as bs]
    [mtgcoll.client.components.cards :refer [->card-list-pager]]
    [mtgcoll.client.utils :refer [get-field-value valid-float? valid-integer?]]))

(defn generic-search-field-element
  [value on-change invalid? on-enter field-def]
  [bs/FormGroup
   (if invalid? {:validation-state "error"})
   [bs/FormControl
    {:type      "text"
     :value     value
     :on-change #(on-change (get-field-value %))
     :on-focus  (fn [e]
                  (let [target (.-target e)]
                    (js/setTimeout #(.select target) 0)))
     :on-key-up (fn [e]
                  (if (= 13 (.-keyCode e))
                    (if on-enter (on-enter))))}]
   (if invalid? [bs/FormControl.Feedback])])

(defn boolean-search-field-element
  [value on-change invalid? on-enter field-def]
  (let [on-change (fn [e]
                    (let [checked?      (-> e .-target .-checked)
                          checked-value (-> e .-target .-value)]
                      (on-change (and checked?
                                      (= "true" checked-value)))))]
    [bs/FormGroup
     (if invalid? {:validation-state "error"})
     [bs/Radio
      (merge
        {:inline true
         :value "true"
         :on-change on-change}
        (if value {:checked true}))
      "Yes"]
     [bs/Radio
      (merge
        {:inline true
         :value "false"
         :on-change on-change}
        (if-not value {:checked true}))
      "No"]
     ]))

(defn checkboxes-search-field-element
  [value on-change invalid? on-enter {:keys [choices] :as field-def}]
  [bs/FormGroup
   (if invalid? {:validation-state "error"})
   (map
     (fn [choice]
       (let [[check-label check-value] (if (vector? choice) choice [choice choice])]
         ^{:key check-value}
         [bs/Checkbox
          (merge
            {:inline    true
             :value     check-value
             :on-change (fn [e]
                          (let [value         (set value)
                                checked?      (-> e .-target .-checked)
                                checked-value (-> e .-target .-value)
                                new-value     (if checked?
                                                (conj value checked-value)
                                                (disj value checked-value))]
                            (on-change new-value)))}
            (if (contains? value check-value) {:checked true}))
          check-label]))
     choices)])

(defvc set-search-field-element
  [value on-change invalid? on-enter field-def]
  (let [sets (view-cursor :simple-sets-list)]
    [bs/FormGroup
     (if invalid? {:validation-state "error"})
     [bs/FormControl
      {:component-class "select"
       :value           value
       :on-change       #(on-change (get-field-value %))
       :on-key-up       (fn [e]
                          (if (= 13 (.-keyCode e))
                            (if on-enter (on-enter))))}
      (->> @sets
           (map
             (fn [{:keys [code name]}]
               ^{:key code} [:option {:value code} name]))
           (cons ^{:key ""} [:option "Select a Set"]))]
     (if invalid? [bs/FormControl.Feedback])]))

(def comparisons
  {:=    "Equal To"
   :not= "Not Equal To"
   :like "Contains"
   :>    "Greater Than"
   :<    "Less Than"})

(def search-filter-defs
  {:name           {:label "Name" :type :text :comparisons [:like :=]}
   :set-code       {:label "Set" :type :text :comparisons [:=] :component set-search-field-element}
   :colors         {:label "Colors" :type :checkbox :comparisons [:like :=] :choices ["Black" "Blue" "Green" "Red" "White"] :component checkboxes-search-field-element}
   :color-identity {:label "Color Identity" :type :checkbox :comparisons [:like :=] :choices [["Black" "B"] ["Blue" "U"] ["Green" "G"] ["Red" "R"] ["White" "W"]] :component checkboxes-search-field-element}
   :type           {:label "Type" :type :text :comparisons [:like :=]}
   :text           {:label "Card Text" :type :text :comparisons [:like :=]}
   :artist         {:label "Artist" :type :text :comparisons [:like :=]}
   :number         {:label "Card Number" :type :text :comparisons [:=]}
   :power          {:label "Power" :type :text :comparisons [:=]}
   :toughness      {:label "Toughness" :type :text :comparisons [:=]}
   :owned?         {:label "Owned?" :type :boolean :comparisons [:=]}
   :owned-count    {:label "Owned Amount" :type :numeric :comparisons [:= :> :<] :validation-fn valid-integer? :transform-fn js/parseInt}
   :paper-price    {:label "Price (Paper)" :type :numeric :comparisons [:= :> :<] :validation-fn valid-float? :transform-fn js/parseFloat}
   :online-price   {:label "Price (Online)" :type :numeric :comparisons [:= :> :<] :validation-fn valid-float? :transform-fn js/parseFloat}})

(defn ->search-field-map
  [field & [filter-id]]
  (let [field (keyword field)
        field-def (get search-filter-defs field)
        field-type (:type field-def)]
    (if field-def
      (merge
        {:field         field
         :type          field-type
         :comparison    (first (:comparisons field-def))
         :value         (case field-type
                          :boolean  false
                          :numeric  0
                          :text     ""
                          :checkbox #{})
         :invalid?      false
         :validation-fn (:validation-fn field-def)
         :transform-fn  (:transform-fn field-def)
         :component     (or (:component field-def)
                            (case field-type
                              :boolean boolean-search-field-element
                              generic-search-field-element))}
        (if filter-id
          {:filter-id filter-id})))))

(defn set-search-field!
  [filter field]
  (if-let [field-map (->search-field-map field)]
    (swap! filter merge field-map)
    (swap! filter #(select-keys % [:filter-id]))))

(defn update-search-field-value!
  [filter value]
  (let [validation-fn (or (:validation-fn @filter) (constantly true))]
    (swap! filter assoc
           :invalid? (not (validation-fn value))
           :value value)))

(defn search-field
  [filter existing-search-fields remove-button on-enter]
  (let [field             (:field @filter)
        field-type        (:type @filter)
        invalid?          (:invalid? @filter)
        input-comp        (:component @filter)
        field-comparisons (:comparisons (get search-filter-defs field))]
    [bs/Row
     [bs/Col {:sm 2} remove-button]
     [bs/Col {:sm 2}
      [bs/FormControl
       {:component-class "select"
        :value           (or field "")
        :on-change       #(set-search-field! filter (get-field-value %))}
       (->> search-filter-defs
            (remove
              #(and (contains? existing-search-fields (first %))
                    (not= (:field @filter) (first %))))
            (map (fn [[k v]] [k (:label v)]))
            (sort-by second)
            (map
              (fn [[field field-label]]
                ^{:key field} [:option {:value field} field-label]))
            (cons ^{:key ""} [:option "Select a field"]))]]
     (if field
       [bs/Col {:sm 2}
        (if (> (count field-comparisons) 1)
          [bs/FormControl
           {:component-class "select"
            :value           (:comparison @filter)
            :on-change       #(swap! filter assoc-in [:comparison] (keyword (get-field-value %)))}
           (map
             (fn [comparison]
               ^{:key comparison} [:option {:value comparison} (get comparisons comparison)])
             field-comparisons)]
          [bs/FormControl.Static (get comparisons (:comparison @filter))])])
     (if field
       [bs/Col {:sm (case field-type
                      :text     5
                      :numeric  2
                      :checkbox 6
                      5)}
        [input-comp
         (:value @filter)
         #(update-search-field-value! filter %)
         invalid?
         on-enter
         (get search-filter-defs field)]])]))

(defn- add-filter!
  [search-filters]
  (let [id (:next-id (swap! search-filters update-in [:next-id] inc))]
    (swap! search-filters
           (fn [{:keys [filters] :as search-filters}]
             (let [new-filter (if (empty? filters)
                                (->search-field-map :name id)
                                {:filter-id id})]
               (assoc-in search-filters [:filters id] new-filter))))))

(defn- remove-filter!
  [{:keys [filter-id] :as filter} search-filters]
  (swap! search-filters update-in [:filters] #(dissoc % filter-id)))

(defn ->active-search-filters
  [working-filters]
  (->> working-filters
       (remove :invalid?)
       (remove #(not (contains? search-filter-defs (:field %))))
       (map
         (fn [{:keys [field value comparison transform-fn]}]
           {:field      field
            :value      (if transform-fn (transform-fn value) value)
            :comparison comparison}))
       (remove empty?)
       (vec)))

(defn ->search-filters
  [& selected-fields]
  (let [next-id (count selected-fields)
        filters (->> selected-fields
                     (map-indexed
                       (fn [idx field]
                         [idx (->search-field-map field idx)]))
                     (reduce #(assoc %1 (first %2) (second %2)) {}))]
    {:next-id        next-id
     :filters        filters
     :active-filters []
     :pager          (->card-list-pager)}))

(defn reset-search-filters!
  [search-filters & [fixed-filters]]
  (reset! search-filters
          (merge
            (->search-filters)
            (if fixed-filters
              {:active-filters fixed-filters}))))

(defn apply-search-filters!
  [search-filters & [fixed-filters]]
  (swap! search-filters
         #(merge %
                 {:active-filters (-> (->active-search-filters (vals (:filters %)))
                                      (into fixed-filters))
                  :pager          (->card-list-pager)})))

(defn get-selected-search-fields
  [search-filters]
  (->> search-filters
       :filters
       (vals)
       (map :field)
       (remove nil?)
       (set)
       (doall)))

(defn search-filter-selector
  [search-filters & [{:keys [fixed-active-filters no-filters-message?] :as options}]]
  (let [fixed-filter-fields    (mapv :field fixed-active-filters)
        selected-search-fields (-> (get-selected-search-fields @search-filters)
                                   (into fixed-filter-fields))
        filters                (->> (:filters @search-filters)
                                    (sort-by :filter-id))]
    [bs/Grid {:fluid true :class "search-filters"}
     (if (and no-filters-message? (empty? filters))
       [bs/Row
        [bs/Alert {:bsStyle "info"}
         "No search filters. Click the " [bs/Glyphicon {:glyph "plus"}] " button below to add some."]])
     (map
       (fn [[filter-id filter]]
         ^{:key filter-id}
         [search-field
          (r/cursor search-filters [:filters filter-id])
          selected-search-fields
          [bs/Button
           {:block    true
            :bsStyle  "danger"
            :on-click #(remove-filter! filter search-filters)}
           [bs/Glyphicon {:glyph "minus"}] " Remove Filter"]
          #(apply-search-filters! search-filters fixed-active-filters)])
       filters)
     [bs/Row
      [bs/Col {:sm 2}
       [bs/Button
        {:block    true
         :on-click #(add-filter! search-filters)}
        [bs/Glyphicon {:glyph "plus"}] " Add Filter"]]
      [bs/Col {:sm 6}
       [bs/Button
        {:bsStyle  "primary"
         :on-click #(apply-search-filters! search-filters fixed-active-filters)}
        [bs/Glyphicon {:glyph "search"}] " Apply Filters"]
       " "
       [bs/Button
        {:bsStyle "warning"
         :on-click #(reset-search-filters! search-filters fixed-active-filters)}
        "Reset"]]]]))