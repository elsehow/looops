(ns looops.sounds
  (:require [goog.net.XhrIo]
            [cljs.core.async :as async :refer [<! >! chan close!]]
            [hum.core :as hum])
  (:require-macros [cljs.core.async.macros :refer [go]]))

; web audio setup

(def context
  (hum/create-context))

; helper functions

(defn- now []
  (.-currentTime context))

(defn- new-gain-node []
  "Makes a new gain node on the audio context"
  (hum/create-gain context))

(defn- new-buffer-node [buffer]
  "Makes a new buffer node from the given AudioBuffer"
  (let [source (hum/create-buffer-source context buffer)]
    (set! (.-loop source) true)
    source))

(defn wire-nodes [source gain]
  "Connects source node to gain node, and gain node to output."
  (hum/connect source gain)
  (hum/connect-output gain))

(defn- schedule-fade [gain-node level ms]
  "Fades gain-node to level some ms in the future"
  (let [later (+ (now) (/ ms 1000))]
    (-> (.-gain gain-node)
        (.linearRampToValueAtTime level later))))

(defn- fade-in-gain-node [gain ms]
  "Fades gain node in (to max gain) over given ms"
  (schedule-fade gain 0 0)
  (schedule-fade gain 1 ms))

(defn- fade-out-gain-node [gain ms]
  "Fades gain node out (to 0 gain) over given ms"
  (schedule-fade gain 0 ms))

(defn- stop-source [source]
  (.stop source 0))

(defn- start-source [source]
  (.start source 0))
;
;
;(defn percent-played [buffer start-time]
;  (let [duration (.-duration buffer)
;        ago (- (now) start-time)]
;    (-> ago (mod duration) (/ duration))))

; async fetch stuff

(defn- decode-audio-data [data]
  (let [ch (chan)]
    (.decodeAudioData context
                      data
                      (fn [buffer]
                        (go (>! ch buffer)
                            (close! ch))))
    ch))

(defn- get-audio [url]
  (let [ch (chan)
        publish-event #(let [res (-> %1 .-target .getResponse)]
                         (go (>! ch res)
                             (close! ch)))]
    (doto (goog.net.XhrIo.)
      (.setResponseType "arraybuffer")
      (.addEventListener goog.net.EventType.COMPLETE
                         publish-event)
      (.send url "GET"))
    ch))

; public functions

(defn fetch-buffer [url cb]
  (let [ch (chan)]
    (go
      (let [response (<! (get-audio url))
            buffer (<! (decode-audio-data response))]
        (cb buffer)))))

(defn make-audio-nodes [buffer]
  "Takes an AudioBuffer and returns a hashmap {:buffer-node :gain-node}
   where gain-node is connected to audio context"
  (let [buffer (new-buffer-node buffer)
        gain (new-gain-node)]
    (wire-nodes buffer gain)
    {:buffer-node buffer
     :gain-node gain}))
        
; TODO more drY?

(defn fade-in [audio-nodes fade-time]
  "Starts looping and fades in audio nodes {:gain-node :buffer-node}, over fade time"
  (let [{:keys [gain-node buffer-node]} audio-nodes]
    (start-source buffer-node)
    (fade-in-gain-node gain-node fade-time)))

(defn fade-out [audio-nodes fade-time]
  "Fades out audio nodes {:gain-node :buffer-node} and stops looping, over fade time"
  (let [{:keys [gain-node buffer-node]} audio-nodes]
    (js/setTimeout #(stop-source buffer-node) fade-time)
    (fade-out-gain-node gain-node fade-time)))
