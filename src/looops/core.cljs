(ns ^:figwheel-always looops.core
    (:require
     [looops.session :as session]
     [reagent.core :as reagent :refer [atom]]
     [secretary.core :as secretary :refer-macros [defroute]]
     [looops.views.feed :refer [feed-view]]
     [looops.views.upload :refer [upload-view]]
     [goog.events :as events]
     [goog.history.EventType :as EventType])
    (:import goog.History))

(enable-console-print!)

(let [history (History.)
      navigation EventType/NAVIGATE]
  (goog.events/listen history
                      navigation
                      #(-> % .-token secretary/dispatch!))
  (doto history (.setEnabled true)))

(defroute "/" []
  (session/put! :current-view feed-view))

(defroute "/upload" []
  (session/put! :current-view upload-view))

(defn page []
  [(session/get :current-view)])

(defn init! []
  (secretary/set-config! :prefix "#")
  (session/put! :current-view feed-view)
  (reagent/render-component
   [page]
   (.getElementById js/document "app")))

(init!)
