(ns mtgcoll.client.routes.lists
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.reagent.bootstrap :as bs]
    [webtools.cljs.ajax :as ajax]
    [webtools.cljs.utils :refer [->url redirect!]]
    [mtgcoll.common :as c]
    [mtgcoll.client.auth :as auth]
    [mtgcoll.client.page :refer [set-active-breadcrumb! set-error!]]
    [mtgcoll.client.utils :refer [get-field-value format-date]]
    [mtgcoll.client.components.cards :refer [card-list-table ->card-list-pager]]
    [mtgcoll.client.components.search :as s]
    [mtgcoll.client.components.utils :refer [click-to-edit-textarea markdown confirm-modal]]))

(defn create-list-form
  [visibility-atom]
  (let [values    (r/atom {:public?             true
                           :requires-qualities? true})
        error     (r/atom nil)
        on-close  (fn []
                    (reset! values nil)
                    (reset! error nil)
                    (reset! visibility-atom false))
        on-submit (fn []
                    (reset! error nil)
                    (let [{:keys [name requires-qualities? public?]} @values]
                      (if (string/blank? name)
                        (reset! error "List name must be provided.")
                        (ajax/POST (->url "/lists/add")
                                   :params {:name name :public? public? :requires-qualities? requires-qualities?}
                                   :on-error #(reset! error "Could not create list. Make sure list name is unique.")
                                   :on-success (fn [response]
                                                 (let [new-list-id (:id (clojure.walk/keywordize-keys response))]
                                                   (redirect! (str "#/list/" new-list-id))))))))
        on-key-up #(if (= 13 (.-keyCode %))
                    (on-submit))]
    (fn []
      [bs/Modal
       {:show    (boolean @visibility-atom)
        :on-hide #(reset! visibility-atom false)}
       [bs/Modal.Header [bs/Modal.Title "Create List"]]
       [bs/Modal.Body
        (if @error
          [bs/Alert {:bsStyle "danger"} @error])
        [bs/Form {:horizontal true}
         [bs/FormGroup
          [bs/Col {:class "text-right" :sm 4} [bs/ControlLabel "List Name"]]
          [bs/Col {:sm 6}
           [bs/FormControl
            {:type          "text"
             :default-value (or (:name @values) "")
             :on-change     #(swap! values assoc :name (get-field-value %))
             :on-key-up     on-key-up}]]]
         [bs/FormGroup
          [bs/Col {:class "text-right" :sm 4} [bs/ControlLabel "Card Qualities"]]
          [bs/Col {:sm 6}
           [bs/Checkbox
            (merge
              (if (:requires-qualities? @values) {:checked true})
              {:on-change (fn [e]
                            (let [checked? (-> e .-target .-checked)]
                              (swap! values assoc :requires-qualities?
                                     (if checked? true false))))})]]]
         (if (auth/auth-required?)
           [bs/FormGroup
            [bs/Col {:class "text-right" :sm 4} [bs/ControlLabel "Public"]]
            [bs/Col {:sm 6}
             [bs/Checkbox
              (merge
                (if (:public? @values) {:checked true})
                {:on-change (fn [e]
                              (let [checked? (-> e .-target .-checked)]
                                (swap! values assoc :public?
                                       (if checked? true false))))})]]])]]
       [bs/Modal.Footer
        [bs/Button {:bsStyle "primary" :on-click on-submit} "OK"]
        [bs/Button {:on-click on-close} "Cancel"]]])))

(defn rename-list-form
  [visibility-atom list-id list-name]
  (let [values    (r/atom {:name list-name})
        error     (r/atom nil)
        on-close  (fn []
                    (reset! values nil)
                    (reset! error nil)
                    (reset! visibility-atom false))
        on-submit (fn []
                    (reset! error nil)
                    (let [{:keys [name]} @values]
                      (if (string/blank? name)
                        (reset! error "List name must be provided.")
                        (ajax/POST (->url "/lists/update-name")
                                   :params {:list-id list-id :name name}
                                   :on-error #(reset! error "Could not update list name. Make sure the name is unique.")
                                   :on-success #(on-close)))))
        on-key-up #(if (= 13 (.-keyCode %))
                    (on-submit))]
    (fn []
      [bs/Modal
       {:show    (boolean @visibility-atom)
        :on-hide #(reset! visibility-atom false)}
       [bs/Modal.Header [bs/Modal.Title "Update List Name"]]
       [bs/Modal.Body
        (if @error
          [bs/Alert {:bsStyle "danger"} @error])
        [bs/Form {:horizontal true}
         [bs/FormGroup
          [bs/Col {:class "text-right" :sm 4} [bs/ControlLabel "List Name"]]
          [bs/Col {:sm 6}
           [bs/FormControl
            {:type          "text"
             :default-value (or (:name @values) "")
             :on-change     #(swap! values assoc :name (get-field-value %))
             :on-key-up     on-key-up}]]]]]
       [bs/Modal.Footer
        [bs/Button {:bsStyle "primary" :on-click on-submit} "OK"]
        [bs/Button {:on-click on-close} "Cancel"]]])))

(defvc copy-list-form
  [visibility-atom source-list-id source-has-qualities?]
  (let [values    (r/atom {:destination-list-id nil})
        error     (r/atom nil)
        on-close  (fn []
                    (reset! values nil)
                    (reset! error nil)
                    (reset! visibility-atom false))
        on-submit (fn []
                    (reset! error nil)
                    (let [{:keys [destination-list-id]} @values]
                      (if (string/blank? name)
                        (reset! error "Copy destination list must be selected.")
                        (ajax/POST (->url "/collection/copy-list")
                                   :params {:source-list-id      source-list-id
                                            :destination-list-id destination-list-id}
                                   :on-error #(reset! error "Error copying the list.")
                                   :on-success #(on-close)))))
        on-key-up #(if (= 13 (.-keyCode %))
                     (on-submit))]
    (fn []
      (let [lists (view-cursor :lists-list (auth/get-username))]
        [bs/Modal
         {:show    (boolean @visibility-atom)
          :on-hide #(reset! visibility-atom false)}
         [bs/Modal.Header [bs/Modal.Title "Copy to Other List"]]
         [bs/Modal.Body
          (if @error
            [bs/Alert {:bsStyle "danger"} @error])
          [bs/Form {:horizontal true}
           [bs/FormGroup
            [bs/Col {:class "text-right" :sm 4} [bs/ControlLabel "Destination"]]
            [bs/Col {:sm 6}
             [bs/FormControl
              {:component-class "select"
               :value           (or (:destination-list-id @values) "")
               :on-change       #(let [value (get-field-value %)]
                                   (swap! values assoc :destination-list-id
                                          (if (string/blank? value)
                                            nil
                                            (int value))))
               :on-key-up       on-key-up}
              (->> @lists
                   (filter
                     (fn [{:keys [id require_qualities]}]
                       (and (not= id source-list-id)
                            (or (and (not source-has-qualities?)
                                     (not require_qualities))
                                source-has-qualities?))))
                   (map
                     (fn [{:keys [id name]}]
                       ^{:key id}
                       [:option {:value id} name]))
                   (cons ^{:key ""} [:option ""]))]]]]]
         [bs/Modal.Footer
          [bs/Button
           {:bsStyle "primary"
            :on-click on-submit
            :disabled (string/blank? (:destination-list-id @values))} "OK"]
          [bs/Button {:on-click on-close} "Cancel"]]]))))


(defvc lists-list
  []
  (let [show-create-form? (r/atom false)]
    (fn []
      (let [lists (view-cursor :lists-list (auth/get-username))]
        (set-active-breadcrumb! :lists)
        [:div.context
         (if (auth/can-modify-data?)
           [:div.absolute.top-right
            [bs/Button {:on-click #(reset! show-create-form? true)} "Create List"]])
         [bs/PageHeader "Lists"]
         [create-list-form show-create-form?]
         (if (vc/loading? lists)
           [:div "Loading ..."]
           (if (empty? @lists)
             [bs/Alert {:bsStyle "warning"} "No lists found."]
             [bs/Table
              {:bordered true :striped true :condensed true :hover true}
              [:thead
               [:tr
                [:th.col-sm-6 "Name"]
                [:th.col-sm-2 "Created At"]
                [:th.col-sm-2 "Card Qualities?"]
                [:th.col-sm-2 "Cards"]]]
              [:tbody
               (doall
                 (map
                   (fn [{:keys [id name is_public require_qualities created_at num_cards]}]
                     ^{:key id}
                     [:tr
                      (if (and (auth/authenticated?) (not is_public))
                        {:class "warning"})
                      [:td [:a {:href (->url "#/list/" id)} [:div name]]]
                      [:td (format-date (new js/Date created_at))]
                      [:td (if require_qualities "Yes" "")]
                      [:td num_cards]])
                   @lists))]]))]))))

(defn on-update-list-notes!
  [list-id notes]
  (ajax/POST (->url "/lists/update-note")
             :params {:list-id list-id :note notes}
             :on-error #(set-error! "Server error while updating list notes.")))

(defn change-list-visibility!
  [list-id public?]
  (ajax/POST (->url "/lists/update-visibility")
             :params {:list-id list-id :public? public?}
             :on-error #(set-error! "Server error while updating list public/private visibility.")))

(defn delete-list!
  [list-id]
  (ajax/POST (->url "/lists/remove")
             :params {:list-id list-id}
             :on-error #(set-error! "Server error while deleting the list.")
             :on-success #(redirect! "#/lists")))

(defonce list-cards-search-filters
  (r/atom (s/->search-filters)))

(defonce limit-to-list? (r/atom true))

(defn list-cards-list
  [list-id]
  (let [fixed-filters         (if @limit-to-list? [{:field :owned? :value true :comparison :=}] [])
        active-search-filters (r/cursor list-cards-search-filters [:active-filters])
        pager                 (r/cursor list-cards-search-filters [:pager])]
    (s/apply-search-filters! list-cards-search-filters fixed-filters)
    (fn [list-id]
      (let [fixed-filters (if @limit-to-list? [{:field :owned? :value true :comparison :=}] [])]
        [:div.list-cards-list
         [s/search-filter-selector list-cards-search-filters
          {:fixed-active-filters fixed-filters
           :exclude-filters      [:owned? :owned-foil?]
           :extra                [:span {:style {:margin-left "40px"}}
                                  [bs/Checkbox
                                   (merge
                                     (if @limit-to-list? {:checked true})
                                     {:inline    true
                                      :value     true
                                      :on-change (fn [e]
                                                   (let [value (boolean (get-field-value e))]
                                                     (reset! limit-to-list? value)
                                                     (swap! active-search-filters
                                                            (fn [active-filters]
                                                              (->> active-filters
                                                                   (remove #(= :owned? (:field %)))
                                                                   (into (if value
                                                                           [{:field :owned? :value true :comparison :=}]
                                                                           [])))))))})
                                   "Limit To List Cards Only?"]]}]
         [card-list-table list-id @active-search-filters pager {:no-owned-highlight? @limit-to-list?}]]))))

(defvc list-details
  [list-id]
  (let [show-delete-confirm? (r/atom false)
        show-rename-form? (r/atom false)
        show-copy-form? (r/atom false)]
    (fn [list-id]
      (set-active-breadcrumb! :lists)
      (let [list (view-cursor :list-info list-id (auth/get-username))]
        (cond
          (and (not (vc/loading? list))
               (nil? @list))
          [:div "List not found."]

          (vc/loading? list)
          [:div "Loading ..."]

          :else
          [:div.context
           [:div.absolute.top-right
            [:span
             [bs/Button
              {:bsStyle "info"
               :href    (->url "#/stats/list/" list-id)} "List Statistics"]
             " "]
            (if (auth/can-modify-data?)
              [:span
               [bs/DropdownButton {:title "Actions"}
                [bs/MenuItem
                 {:on-click #(reset! show-copy-form? true)}
                 "Copy To Other List"]
                (if (not= 0 list-id)
                  [bs/MenuItem
                   {:on-click #(reset! show-rename-form? true)}
                   "Change List Name"])
                (if (and (auth/auth-required?)
                         (not= 0 list-id))
                  [bs/MenuItem
                   {:on-click #(change-list-visibility! list-id (not (:is_public @list)))}
                   (if (:is_public @list) "Make Private" "Make Public")])
                (if (not= 0 list-id)
                  [bs/MenuItem
                   {:on-click #(reset! show-delete-confirm? true)}
                   "Delete"])]])]
           [bs/PageHeader (:name @list)
            (if (auth/can-modify-data?)
              [:span
               " "
               (if-not (:is_public @list) [:span.large-font [bs/Label {:bsStyle "danger"} "Private"] " "])
               (if (:require_qualities @list) [:span.large-font [bs/Label {:bsStyle "primary"} "Card Qualities"] " "])])]
           (if @show-copy-form?
             [copy-list-form show-copy-form? list-id (:require_qualities @list)])
           (if @show-rename-form?
             [rename-list-form show-rename-form? list-id (:name @list)])
           [confirm-modal
            show-delete-confirm?
            {:title "Confirm Delete"
             :body [:p "Are you sure you want to delete the " [:strong (:name @list)] " list? This cannot be undone."]
             :on-yes #(delete-list! list-id)}]
           [:div.list-details
            (if (auth/can-modify-data?)
              [click-to-edit-textarea
               (:notes @list)
               {:placeholder "List Notes"
                :rows 10
                :on-update #(on-update-list-notes! list-id %)
                :render    markdown}]
              [markdown (:notes @list)])]
           [list-cards-list list-id]])))))