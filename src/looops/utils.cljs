(ns looops.utils
 (:require
     [looops.session :as session]
     [cljs.core.async :as async :refer [<! >! chan close!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn decode-audio-data [data]
  (let [ch (chan)]
    (.decodeAudioData session/context
                      data
                      (fn [buffer]
                        (go (>! ch buffer)
                            (close! ch))))
    ch))

(defn get-audio [url]
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

(defn fetch-buffer [url]
  (let [ch (chan)]
    (go
      (let [response (<! (get-audio url))
            buffer (<! (decode-audio-data response))]
        (>! ch buffer)
        (close! ch)))
      ch))
