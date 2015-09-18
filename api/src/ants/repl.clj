(ns ants.repl
  (:require [ants.api.core :as api]
            [ants.engine.core :as engine]))

(def world engine/*world*)
(def server (atom nil))
(defn start [] (reset! server (api/server world)))
(defn stop [] (@server))
(defn reload [] (require '[ants.api.core :as api :reload-all true]))

(defn inspect []
  (let [stuff (vals @(.stuff world))
        ants (filter #(= :ant (:type %)) stuff)
        food (filter #(= :food (:type %)) stuff)]
    (doseq [l (take 10 (reverse @(.log world)))]
      (println l))
    (doseq [a ants]
      (prn a))
    (doseq [f food]
      (prn f))))

#(do (engine/place-food world [-1 1]) (inspect))
#(do (engine/remove-food world [-1 -1]) (inspect))

(defn scores []
  (let [stuff (vals @(.stuff world))
        ants (filter #(= :ant (:type %)) stuff)
        ants (reverse (sort-by :points ants))]
    (println "Ant Ranking by Points")
    (doseq [a ants]
      (println (:points a) (:name a) (:location a)))))