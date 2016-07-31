(ns mtgcoll.client.components.utils
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [cljsjs.showdown]
    [webtools.reagent.bootstrap :as bs]
    [webtools.reagent.components :as c]
    [webtools.cljs.utils :refer [->url]]
    [mtgcoll.client.utils :refer [get-field-value]]))

(defn symbol-image
  [symbol & {:keys [size]}]
  (let [size (or size "small")]
    [:img {:src (->url "/images/symbols/" size "/" symbol)}]))

(defn set-image
  [set-code & {:keys [size]}]
  (let [size (or size "small")]
    [:img {:src (->url "/images/sets/" size "/" set-code)}]))

(defn set-short-label
  [set-code & [set-name]]
  (let [el [:span [set-image set-code] " " set-code]]
    (if set-name
      [bs/OverlayTrigger
       {:animation false
        :placement "top"
        :overlay (r/as-component [bs/Tooltip set-name])}
       el]
      el)))

(defn set-label
  [set-code set-name]
  [:span [set-image set-code] " " set-name])

(defn set-heading
  [set-code set-name]
  [:span [set-image set-code :size "medium"] " " set-name])

(defn symboled-markup
  [s & {:keys [size italicize? split-paragraphs? wrap-with]}]
  (let [size (or size "small")]
    (if (string/blank? s)
      [:span s]
      [c/raw-html :span
       (as-> (string/trim s) x
             (if (and split-paragraphs?
                      (string/includes? x "\n"))
               (as-> x y
                     (string/split y #"\n")
                     (map
                       (fn [paragraph]
                         (r/render-to-string
                           (if wrap-with
                             [:p [c/raw-html wrap-with paragraph]]
                             [c/raw-html :p paragraph])))
                       y)
                     (string/join y))
               (if wrap-with
                 (r/render-to-string [wrap-with x])
                 x))
             (if italicize?
               (string/replace
                 x #"\((.*)\)"
                 (fn [[_ text]]
                   (r/render-to-string [c/raw-html :em (str "(" text ")")])))
               x)
             (string/replace
               x #"\{([\w/]+)\}"
               (fn [[_ symbol]]
                 (let [symbol (string/replace symbol "/" "")]
                   (r/render-to-string [symbol-image symbol :size size])))))])))

(defn th-sortable
  [sort-settings field-name & components]
  (let [sorting-on-this? (= field-name (:sort-by @sort-settings))
        ascending?       (boolean (:ascending? @sort-settings))]
    [:th
     [:a
      {:href     "#"
       :on-click (fn [e]
                   (swap! sort-settings
                          (fn [{:keys [ascending?] :as sort-settings}]
                            (assoc sort-settings
                              :sort-by field-name
                              :ascending? (if sorting-on-this? (not ascending?) true))))
                   (.preventDefault e))}
      [:div
       components
       (if sorting-on-this?
         [c/raw-html :span (if ascending? "&#9650;" "&#9660;")])]]]))

(def ^:private markdown-converter (new js/showdown.Converter))

(defn markdown
  [s]
  [c/raw-html :span (.makeHtml markdown-converter s)])

(defn click-to-edit-textarea
  [value & [{:keys [placeholder rows on-update render]}]]
  (let [editing?      (r/atom false)
        current-value (r/atom value)]
    (fn [value & [{:keys [placeholder rows]}]]
      [bs/FormGroup
       (if-not @editing?
         [bs/FormControl.Static
          {:on-mouse-down #(reset! editing? true)}
          (let [content (or @current-value placeholder)]
            (if render
              (render content)
              content))]
         [bs/FormControl
          {:component-class "textarea"
           :ref             #(if % (.focus (r/dom-node %)))
           :rows            rows
           :default-value   @current-value
           :placeholder     placeholder
           :on-blur         (fn [_]
                              (let [value (string/trim (str @current-value))]
                                (reset! editing? false)
                                (if on-update (on-update (if (string/blank? value) nil value)))))
           :on-change       #(reset! current-value (get-field-value %))}])])))

(defn confirm-modal
  [visibility-atom {:keys [title body yes-label no-label on-yes on-no]}]
  [bs/Modal
   {:show    (boolean @visibility-atom)
    :on-hide #(reset! visibility-atom false)}
   [bs/Modal.Header [bs/Modal.Title title]]
   [bs/Modal.Body body]
   [bs/Modal.Footer
    [bs/Button
     {:bsStyle  "primary"
      :on-click (fn [_]
                  (on-yes)
                  (reset! visibility-atom false))}
     (or yes-label "Yes")]
    [bs/Button
     {:on-click (fn [_]
                  (if on-no (on-no))
                  (reset! visibility-atom false))}
     (or no-label "No")]]])
