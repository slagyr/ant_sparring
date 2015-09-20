(ns ants.repl
  (:require [ants.api :as api]
            [ants.engine :as engine]
            [ants.gui :as gui]))

(def world engine/*world*)
(def server (atom nil))
(defn start [] (reset! server (api/server world)))
(defn stop [] (@server))
(defn reload [] (require '[ants.api.api :as api :reload-all true]))

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

(defn scores []
  (let [stuff (vals @(.stuff world))
        nests (filter #(= :nest (:type %)) stuff)
        nests (reverse (sort-by :food nests))]
    (println "Nest Ranking by Points")
    (doseq [a nests]
      (println (:food a) (:team a)))))

(defn show []
  (if @gui/w
    (gui/update)
    (gui/show world)))

(defn place-random-food []
  (let [x (rand-nth (range -10 11))
        y (rand-nth (range -10 11))]
    (engine/place-food world [x y])
    [x y]))

(defn remove-random-food []
  (let [foods (filter #(= :food (:type %)) (vals @(.stuff world)))]
    (when (seq foods)
      (let [food (rand-nth foods)]
        (engine/remove-food world (:location food))
        food))))