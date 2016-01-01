(ns looops.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

;; -------------------------
;; Views

(defn menu []
  [:ul
   [:li [:a {:href "/"} "home"]]
   [:li [:a {:href "/upload"} "upload"]]])

(defn home-page []
  [:div [:h2 "Welcome to looops"]])

(defn upload-page []
  [:div [:h2 "upload looops"]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/upload" []
  (session/put! :current-page #'upload-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render
   [:div
    [menu]
    [current-page]]
   (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!)
  (accountant/dispatch-current!)
  (mount-root))
