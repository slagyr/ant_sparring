(ns ants.gui
  (:import (javax.swing JFrame JPanel WindowConstants)
           (java.awt Dimension Color Graphics)))

(def COLORS (cycle [Color/RED Color/ORANGE Color/YELLOW Color/BLUE Color/CYAN Color/MAGENTA Color/PINK]))

(defn paint-cells [graphics color cells]
  (.setColor graphics color)
  (doseq [t cells]
    (let [[x y] (:location t)]
      (.fillRect graphics (+ 250 (* x 10)) (+ 250 (* y 10)) 10 10))))

(defn world-panel [world]
  (proxy [JPanel] []
    (paintComponent [^Graphics graphics]
      (.setColor graphics Color/GRAY)
      (doseq [x (range 10 510 10)]
        (.drawLine graphics x 0 x 510)
        (.drawLine graphics 0 x 510 x))
      (let [ants (filter #(= :ant (:type %)) (vals @(.stuff world)))
            teams (sort-by first (group-by :team ants))
            colored-ants (map (fn [[_ ants] color] [color ants]) teams COLORS)]
        (doseq [[color ants] colored-ants]
          (paint-cells graphics color ants)))
      (paint-cells graphics (Color/GREEN) (filter #(= :food (:type %)) (vals @(.stuff world)))))))

(def w (atom nil))

(defn show [world]
  (let [window (JFrame. "Ants")
        panel (world-panel world)]
    (reset! w panel)
    (.setDefaultCloseOperation window WindowConstants/DISPOSE_ON_CLOSE)
    (.setPreferredSize panel (Dimension. 510 510))
    (.add (.getContentPane window) panel)
    (.pack window)
    (.setVisible window true)))

(defn update []
  (.repaint @w))

