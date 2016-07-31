(ns mtgcoll.client.routes.lists
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]
    [webtools.reagent.bootstrap :as bs]
    [webtools.cljs.ajax :as ajax]
    [webtools.cljs.utils :refer [->url redirect!]]
    [mtgcoll.client.auth :as auth]
    [mtgcoll.client.page :refer [set-active-breadcrumb! set-error!]]
    [mtgcoll.client.utils :refer [get-field-value]]
    [mtgcoll.client.components.utils :refer [click-to-edit-textarea markdown confirm-modal]]))

(defn create-list-form
  [visibility-atom]
  (let [values    (r/atom nil)
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
            {:on-change (fn [e]
                          (let [checked? (-> e .-target .-checked)]
                            (swap! values assoc :requires-qualities?
                                   (if checked? true false))))}]]]
         [bs/FormGroup
          [bs/Col {:class "text-right" :sm 4} [bs/ControlLabel "Public"]]
          [bs/Col {:sm 6}
           [bs/Checkbox
            {:on-change (fn [e]
                          (let [checked? (-> e .-target .-checked)]
                            (swap! values assoc :public?
                                   (if checked? true false))))}]]]]]
       [bs/Modal.Footer
        [bs/Button {:bsStyle "primary" :on-click on-submit} "OK"]
        [bs/Button {:on-click on-close} "Cancel"]]])))

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
                [:th "Name"]
                [:th "Cards"]]]
              [:tbody
               (doall
                 (map
                   (fn [{:keys [id name is_public]}]
                     ^{:key id}
                     [:tr
                      (if (and (auth/authenticated?) (not is_public))
                        {:class "warning"})
                      [:td [:a {:href (->url "#/list/" id)} [:div name]]]
                      [:td "--"]])
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

(defvc list-details
  [list-id]
  (let [show-delete-confirm? (r/atom false)]
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
           (if (auth/can-modify-data?)
             [:div.absolute.top-right
              (if-not (:is_public @list) [:span.large-font [bs/Label {:bsStyle "danger"} "Private"] " "])
              (if (:require_qualities @list) [:span.large-font [bs/Label {:bsStyle "primary"} "Card Qualities"] " "])
              [bs/DropdownButton {:title "Actions"}
               [bs/MenuItem {:on-click #(js/alert "TODO: Copy to Owned")} "Copy to Owned"]
               [bs/MenuItem {:on-click #(change-list-visibility! list-id (not (:is_public @list)))} (if (:is_public @list) "Make Private" "Make Public")]
               [bs/MenuItem {:on-click #(reset! show-delete-confirm? true)} "Delete"]]])
           [bs/PageHeader (:name @list)]
           (if (auth/can-modify-data?)
             [click-to-edit-textarea
              (:notes @list)
              {:placeholder "List Notes"
               :rows 10
               :on-update #(on-update-list-notes! list-id %)
               :render    markdown}]
             [markdown (:notes @list)])
           [confirm-modal
            show-delete-confirm?
            {:title "Confirm Delete"
             :body [:p "Are you sure you want to delete the " [:strong (:name @list)] " list? This cannot be undone."]
             :on-yes #(delete-list! list-id)}]])))))