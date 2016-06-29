(ns mtgcoll.client.page
  (:require
    [reagent.core :as r]
    [webtools.cljs.dom :as dom]
    [webtools.reagent.bootstrap :as bs]
    [webtools.cljs.utils :refer [->url]]
    [mtgcoll.client.auth :as auth]
    [mtgcoll.client.components.auth :refer [login-form]]))

(defonce error (r/atom nil))

(defn set-error!
  [message]
  (reset! error message))

(defn clear-error!
  []
  (reset! error nil))

(defonce active-breadcrumb (r/atom nil))

(defn set-active-breadcrumb!
  [page-id]
  (reset! active-breadcrumb page-id))

(defn app-body
  [page-component]
  (let [active-breadcrumb @active-breadcrumb]
    [:div#app-body.container
     [bs/Navbar {:inverse true}
      [bs/Navbar.Header
       [bs/Navbar.Brand
        [:a#logo {:href "#/"}
         [:span [:img {:src (->url "/img/mtg_icon.png")}]]
         "Card Collection"]]]
      [bs/Navbar.Collapse
       [bs/Nav
        [bs/NavItem {:href "#/owned" :active (= :owned active-breadcrumb)} "Owned"]
        [bs/NavItem {:href "#/all" :active (= :all active-breadcrumb)} "All"]
        [bs/NavItem {:href "#/sets" :active (= :sets active-breadcrumb)} "Sets"]
        [bs/NavItem {:href "#/stats" :active (= :stats active-breadcrumb)} "Statistics"]]
       (if (auth/auth-required?)
         [bs/Nav {:pull-right true}
          (if (auth/authenticated?)
            [bs/NavDropdown {:title (:username @auth/user-profile)}
             [bs/MenuItem {:on-click #(auth/logout!)} "Logout"]]
            [bs/NavItem {:on-click auth/show-login-form!} "Login"])])]]
     [bs/Modal
      {:show    (boolean @error)
       :on-hide clear-error!}
      [bs/Modal.Header [bs/Modal.Title "Error"]]
      [bs/Modal.Body
       [:p @error]]
      [bs/Modal.Footer
       [bs/Button {:on-click clear-error!} "Close"]]]
     [login-form]
     page-component]))

(defn page
  [page-component]
  (r/render-component [app-body page-component] (dom/element-by-id "app")))

(defn barebones-page
  [page-component]
  (r/render-component page-component (dom/element-by-id "app")))
