(ns looops.session
  (:refer-clojure :exclude [get])
  (:require
   [reagent.core :as reagent :refer [atom cursor]]))

(defonce app-db (atom {:feed 
   {:103050539 {:title "too far"
                 :artist "izaak shapinsky"
                 :songs {:j23958j1 {:song-name "nice hit"
                                    :url "/music/dr.wav"
                                    :source nil
                                    :playing? false
                                    :loading? false
                                    :buffer nil
                                    :fade 1300}
                          :0afd093 {:song-name "good song 2"
                                    :url "/music/dr.wav"
                                    :source nil
                                    :playing? false
                                    :loading? false
                                    :buffer nil
                                    :fade 0}
                          :9035093 {:song-name "truly excellent song"
                                    :url "/music/1.wav"
                                    :source nil
                                    :playing? false
                                    :loading? false
                                    :buffer nil
                                    :fade 5000}}}}}))

(defn cursor-to [keys]
  (cursor app-db keys))

(defn get [k & [default]]
  (clojure.core/get @app-db k default))

(defn put! [k v]
  (swap! app-db assoc k v))

(defn update-in! [ks f & args]
  (clojure.core/swap!
   app-db
   #(apply (partial update-in % ks f) args)))
