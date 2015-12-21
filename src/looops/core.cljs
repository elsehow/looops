(ns ^:figwheel-always looops.core
    (:require
              [reagent.core :as reagent :refer [atom]]
              [looops.fake-data :as fake]))

(defonce feed (atom fake/data))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defn song-view [song]
  [:div
   [:p (:song-name song)]])

(defn album-view [album]
  [:div
   [:h1 (:title album)]
   [:h2 (:artist album)]
   (map song-view (:songs album))])

(defn feed-view []
  [:div
   (map album-view @feed)])

;;(defn hello-world []
;;  [:h1 (:text @app-state)])

(reagent/render-component [feed-view]
                          (. js/document (getElementById "app")))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

