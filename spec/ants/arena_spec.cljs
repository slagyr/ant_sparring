(ns ants.arena-spec
  (:require-macros [speclj.core :refer [describe it should-fail should= should-not= after before]])
  (:require [ants.arena :as arena]
            [speclj.core]))

(describe "Arena"

  (it "select random food location"
    (doseq [_ (range 1000)]
      (let [[x y] (arena/random-food-location)]
        (when-not (<= 4 (Math/abs x))
          (should-fail (str "x: " x)))
        (when-not (<= 4 (Math/abs y))
          (should-fail (str "y: " y))))))

  )



