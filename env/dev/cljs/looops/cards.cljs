(ns looops.cards
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [looops.sounds :as sounds]
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

; song fuctions

(defn set-prop [ratom prop val]
  (swap! ratom assoc prop val))

(defn stop-song [song]
  (let [{:keys [audio-nodes fade]} @song]
    (sounds/fade-out audio-nodes fade)
    (set-prop song :playing false)))

(defn start-song [song]
  (let [{:keys [buffer fade]} @song
        audio-nodes (sounds/make-audio-nodes buffer)]
    (do
     (set-prop song :audio-nodes audio-nodes)
     (sounds/fade-in audio-nodes fade)
     (set-prop song :playing true))))

(defn load-then-play [song]
  (let [{:keys [url]} @song]
    (set-prop song :loading true)
    (sounds/fetch-buffer
     url
     #(do
        (set-prop song :buffer %)
        (start-song song)
        (set-prop song :loading false)))))

(defn song-clicked [song]
  (let [{:keys [playing loading buffer]} @song]
    (cond
      playing (stop-song song)
      loading (println "its loading enhance your calm c:")
      :else (if buffer
              (start-song song)
              (load-then-play song)))))

; song view
  
(defn song-view [song]
  (let [{:keys [name playing loading]} @song]
    [:button
     {:style {:background-color (cond
                                  playing "green"
                                  loading "gray"
                                  :else "white")}
      :on-click #(song-clicked song)}
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
