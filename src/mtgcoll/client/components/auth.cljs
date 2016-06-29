(ns mtgcoll.client.components.auth
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [webtools.cljs.utils :refer [->url]]
    [webtools.cljs.ajax :as ajax]
    [webtools.reagent.bootstrap :as bs]
    [mtgcoll.client.auth :as auth]
    [mtgcoll.client.views :as views]
    [mtgcoll.client.utils :refer [get-field-value]]))

(defn login-form
  []
  (let [values    (r/atom nil)
        error     (r/atom nil)
        on-close  (fn []
                    (reset! values nil)
                    (reset! error nil)
                    (auth/hide-login-form!))
        on-submit (fn []
                    (reset! error nil)
                    (let [{:keys [username password]} @values]
                      (if (or (string/blank? username) (string/blank? password))
                        (reset! error "Username and password cannot be blank.")
                        (ajax/POST (->url "/login")
                                   :params {:username username :password password}
                                   :on-error #(reset! error "Invalid username/password.")
                                   :on-success (fn [_]
                                                 (on-close)
                                                 (views/reconnect!))))))]
    (fn []
      [bs/Modal
       {:show    (boolean @auth/show-login)
        :on-hide on-close}
       [bs/Modal.Header [bs/Modal.Title "Login"]]
       [bs/Modal.Body
        (if @error
          [bs/Alert {:bsStyle "danger"} @error])
        [bs/Form {:horizontal true}
         [bs/FormGroup
          [bs/Col {:class "text-right" :sm 4} [bs/ControlLabel "Username"]]
          [bs/Col {:sm 6}
           [bs/FormControl
            {:type      "text"
             :value     (or (:username @values) "")
             :on-change #(swap! values assoc :username (get-field-value %))}]]]
         [bs/FormGroup
          [bs/Col {:class "text-right" :sm 4} [bs/ControlLabel "Password"]]
          [bs/Col {:sm 6}
           [bs/FormControl
            {:type      "password"
             :value     (or (:password @values) "")
             :on-change #(swap! values assoc :password (get-field-value %))}]]]]]
       [bs/Modal.Footer
        [bs/Button {:bsStyle "primary" :on-click on-submit} "Login"]
        [bs/Button {:on-click on-close} "Cancel"]]])))