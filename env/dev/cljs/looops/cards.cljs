(ns looops.cards
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [looops.handlers :as handlers]
            [looops.core :as core])
  (:require-macros
   [devcards.core
    :as dc
    :refer [defcard defcard-doc defcard-rg deftest]]))

; example song

(defonce song
  (reagent/atom {:name "yoooo"
                 :fade 1000
                 :url "music/1.wav"}))

; song view
  
(defn song-view [song]
  (let [{:keys [name playing loading]} @song]
    [:button
     {:style {:background-color (cond
                                  playing "green"
                                  loading "gray"
                                  :else "white")}
      :on-click #(handlers/song-clicked song)}
     name]))

;; song card

(defcard-rg song-card
  (fn [data-atom _]
    [song-view data-atom])
  song
  {:inspect-data true :history true})

(reagent/render [:div] (.getElementById js/document "app"))

;; remember to run 'lein figwheel devcards' and then browse to
;; http://localhost:3449/cards
