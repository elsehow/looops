(ns looops.sounds
  (:require [goog.net.XhrIo]
            [cljs.core.async :as async :refer [<! >! chan close!]]
            [hum.core :as hum])
  (:require-macros [cljs.core.async.macros :refer [go]]))

; web audio setup

(def context
  "Makes a web audio context. We only need one of these."
  (hum/create-context))

; helper functions

(defn- now []
  "Gets current time, according to web audio context."
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

(defn- fade-in-gain-node [gain-node ms]
  "Fades gain node in (to max gain) over given ms"
  (schedule-fade gain-node 0 0)
  (schedule-fade gain-node 1 ms))

(defn- fade-out-gain-node [gain-node ms]
  "Fades gain node out (to 0 gain) over given ms"
  (let [current-gain (-> gain-node .-gain .-value)]
    (schedule-fade gain-node current-gain 0)
    (schedule-fade gain-node 0 ms)))

(defn- stop-source [source]
  "Stops an audio buffer source."
  (.stop source 0))

(defn- start-source [source]
  "Starts an audio buffer source."
  (.start source 0))

(defn percent-played [buffer start-time]
  "Calculates the percentage of a song played,
   given its audio buffer and the time it started playing."
  (let [duration (.-duration buffer)
        ago (- (now) start-time)]
    (-> ago (mod duration) (/ duration))))

; async fetch stuff

(defn- decode-audio-data [data]
  "Turns an arraybuffer into web audio data."
  (let [ch (chan)]
    (.decodeAudioData context
                      data
                      (fn [buffer]
                        (go (>! ch buffer)
                            (close! ch))))
    ch))

(defn- get-audio [url]
  "Gets an arraybuffer from a url."
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

(defn fetch-buffer [url cb error-cb]
  "Gets some web audio data from a url of a song file."
  (let [ch (chan)]
    (go (try
          (let [response (<? (get-audio url))
                buffer (<? (decode-audio-data response))]
            (cb buffer)))
        (catch js/Error e
          (error-cb e)))))

(defn make-audio-nodes [buffer]
  "Takes an AudioBuffer and returns a hashmap {:buffer-node :gain-node}
   where gain-node is connected to audio context"
  (let [buffer (new-buffer-node buffer)
        gain (new-gain-node)]
    (wire-nodes buffer gain)
    {:buffer-node buffer
     :gain-node gain}))

(defn fade-in [audio-nodes fade-time]
  "Takes a hashmap {:buffer-node :gain-node},
   starts playing the buffer-node, and fades the
   gain-node in over fade-time."
  (let [{:keys [gain-node buffer-node]} audio-nodes]
    (start-source buffer-node)
    (fade-in-gain-node gain-node fade-time)))

(defn fade-out [audio-nodes fade-time cb]
  "Takes a hashmap {:buffer-node :gain-node},
   fades out the gain-node over fade-time, and,
   once it's faded out, stops playing the buffer-node
   and executes callback cb."
  (let [{:keys [gain-node buffer-node]} audio-nodes]
    (js/setTimeout #(do
                      (stop-source buffer-node)
                      (cb))
                   fade-time)
    (fade-out-gain-node gain-node fade-time)))
