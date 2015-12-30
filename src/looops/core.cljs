(ns ^:figwheel-always looops.core
    (:require
     [looops.session :as session]
     [reagent.core :as reagent :refer [render-component]]
     [secretary.core :as secretary :refer-macros [defroute]]
     [looops.views.feed :refer [feed-view]]
     [looops.views.upload :refer [upload-view]]
     [goog.events :refer [listen]]
     [goog.history.EventType :refer [NAVIGATE]])
     ;[devtools.core :as devtools])
    (:import goog.History))

(enable-console-print!)

;; enable history

(let [h (History.)]
  (listen h NAVIGATE
          #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))

;; routes

(defroute "/" []
  (session/put! :current-view feed-view))

(defroute "/upload" []
  (session/put! :current-view upload-view))

;; init

(defn page []
  [(session/get :current-view)])

(defn init! []
  (secretary/set-config! :prefix "#")
  (session/put! :current-view feed-view)
  (render-component
    [page]
    (.getElementById js/document "app")))

(init!)