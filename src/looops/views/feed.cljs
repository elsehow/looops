(ns looops.views.feed
  (:require [goog.net.XhrIo]
            [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :as async :refer [<! >! chan close!]]
            [looops.session :as session])
   (:require-macros [cljs.core.async.macros :refer [go]]))
   ;[hum.core :as hum]
; set up web audio
;(def ctx (hum/create-context))
;
;(defn hook-up-audio [buffer]
;  (.decodeAudioData
;   ctx
;   buffer 
;   #((def sound-source (hum/create-buffer-source ctx %1))
;     (def gain-node (hum/create-gain ctx))
;     (hum/connect sound-source gain-node)
;     (hum/connect-output gain-node))))

(defn decode-audio-data [context data]
  (let [ch (chan)]
    (.decodeAudioData context
                      data
                      (fn [buffer]
                        (go (>! ch buffer)
                            (close! ch))))
    ch))

(defn get-audio [url]
  (let [ch (chan)]
    (doto (goog.net.XhrIo.)
      (.setResponseType "arraybuffer")
      (.addEventListener goog.net.EventType.COMPLETE
                         (fn [event]
                           (let [res (-> event .-target .getResponse)]
                             (go (>! ch res)
                                 (close! ch)))))
      (.send url "GET"))
    ch))

(def context 
  (let [AudioContext (or (.-AudioContext js/window)
                         (.-webkitAudioContext js/window))]
    context (AudioContext.)))

(defn fetch-buffer [url]
  (let [ch (chan)]
    (go
      (let [response (<! (get-audio url))
            buffer (<! (decode-audio-data context response))]
        (>! ch buffer)
        (close! ch)))
      ch))

(defn make-looping-source [buffer]
  (let [source (doto (.createBufferSource context)
                     (aset "buffer" buffer))]
    (.connect source (.-destination context))
    (set! (.-loop source) true)
    source))

; view stuff

; atom to hold local ui state
(defn song-view [album-id song-id]
  (let [song (session/cursor-to [:feed album-id :songs song-id])
        play #(do
                (swap! song assoc :source
                       (make-looping-source (:buffer @song)))
                (.start (:source @song) 0)
                (swap! song assoc :playing? true))
        stop #(do
                (.stop (:source @song) 0)
                (swap! song assoc :playing? false))
        fetch-and-play #(go
                          (let [buffer (<! (fetch-buffer %1))]
                            (swap! song assoc :buffer buffer)
                            (play)))]
    (fn []
      [:div {:key song-id}
       [:button {:on-click #(if (:playing? @song)
                              (stop)
                              (if (nil? (:buffer @song))
                                (fetch-and-play (:url @song))
                                (play)))}
        (:song-name @song)]])))

(defn album-view [id album]
  [:div {:key id}
   [:h1 (:title album)]
   [:h2 (:artist album)]
   (for [[k v] (:songs album)] [song-view id k])])

(defn feed-view []
  (let [feed (session/cursor-to [:feed])]
    [:div (for [[k v] @feed] (album-view k v))]))





