(ns looops.views.feed
  (:require [goog.net.XhrIo]
            [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :as async :refer [<! >! chan close!]]
            [hum.core :as hum]
            [looops.utils :as utils]
            [looops.session :as session])
   (:require-macros [cljs.core.async.macros :refer [go]]))

; view stuff

(defn new-gain-node []
  (hum/create-gain session/context))

(defn new-looping-source [buffer]
  (let [source (hum/create-buffer-source session/context buffer)]
    (set! (.-loop source) true)
    source))

(defn fade [gain val seconds]
  (-> (.-gain gain)
      (.linearRampToValueAtTime val 
        (+ (session/now) (/ seconds 1000)))))

(defn fade-in [gain seconds]
  (fade gain 0 0)
  (fade gain 1 seconds))

(defn fade-out [gain seconds]
  (fade gain 0 seconds))

(defn stop-source [source]
  (.stop source 0))

(defn start-source [source]
  (.start source 0))

(defn wire-nodes [source gain]
   (hum/connect source gain) 
   (hum/connect-output gain))

(defn percent-played [buffer start-time]
  (let [duration (.-duration buffer)
    ago (- (session/now) start-time)]
    (-> ago (mod duration) (/ duration))))

(defn song-view [album-id song-id]
  (let [song (session/cursor-to [:feed album-id :songs song-id])
        set-song #(swap! song assoc %1 %2) ; (set-song :prop val)
        set-percent-played #(set-song :percent-played 
                              (percent-played 
                                (:buffer @song) 
                                (:start-time @song)))
        setup-audio-nodes (fn []
                            (set-song :source (new-looping-source (:buffer @song)))
                            (set-song :gain-node (new-gain-node))
                            (wire-nodes (:source @song) (:gain-node @song)))
        start-looping (fn []
                        (start-source (:source @song))
                        (set-song :start-time (session/now))
                        (set-song :playing? true)
                        (set-song :ui-interval 
                          (js/setInterval set-percent-played 100)))
        stop-looping (fn []
                        (stop-source (:source @song))
                        (set-song :playing? false)
                        (js/clearInterval (:ui-interval @song)))
        play (fn []
                (setup-audio-nodes) 
                (fade-in (:gain-node @song) (:fade @song))
                (start-looping))
        stop #(let [fade (:fade @song)]
                (fade-out (:gain-node @song) fade)
                (js/setTimeout stop-looping fade))
        ; kind of attrocious fn here
        fetch-and-play (fn [url]
                          (set-song :loading? true)
                          (go ; fetch the song
                            (let [buffer (<! (utils/fetch-buffer url))]
                              (set-song :buffer buffer)
                              (set-song :loading? false)
                              (play))))
        handle-click #(if (:playing? @song)
                        (stop)
                        (if (nil? (:buffer @song))
                          (fetch-and-play (:url @song))
                          (play)))]
      [:div {:key song-id}
       [:button {:on-click handle-click
                 :disabled? (:loading? @song)
                 :style {:background-color
                         (if (:playing? @song) "red" "green")}}
        (if (:playing? @song)
          (:percent-played @song)
          (:song-name @song))]]))

(defn album-view [id]
  (let [sv (partial song-view id)
        album (session/cursor-to [:feed id])]
    [:div {:key id}
     [:h1 (:title @album)]
     [:h2 (:artist @album)]
     (doall (map sv (keys (:songs @album))))]))

(defn feed-view []
  (let [feed (session/cursor-to [:feed])]
    [:div (doall (map album-view (keys @feed)))]))
