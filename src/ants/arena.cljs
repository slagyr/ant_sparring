(ns ants.arena
  (:require [ants.remote :as remote]
            [ants.log :as log]
            [cognitect.transit :as transit]
            [goog.events]
            [reagent.core :as reagent]))
(def start-state {:nests {}
                  :cells {}
                  :logs  []})
(defonce state (reagent/atom start-state))
(defonce cells (reagent/cursor state [:cells]))
(defonce logs (reagent/cursor state [:logs]))
(defonce nests (reagent/cursor state [:nests]))

(defonce colors (reagent/atom (cycle ["FireBrick"
                                      "Green"
                                      "Gold"
                                      "DodgerBlue"
                                      "LightPink"
                                      "Chocolate"
                                      "Magenta"
                                      "Honeydew"
                                      "Salmon"
                                      "Lime"
                                      "DarkKhaki"
                                      "Cyan"
                                      "MediumVioletRed"
                                      "Goldenrod"
                                      "DarkMagenta"
                                      "MistyRose"
                                      ])))

(def transit-reader (transit/reader :json))

(defn nest? [thing] (= :nest (:type thing)))
(defn food? [thing] (= :food (:type thing)))
(defn ant? [thing] (= :ant (:type thing)))

(defn color-for [thing]
  (case (:type thing)
    :nest "black"
    :food "grey"
    (when-let [nest (get @nests (:nest thing))]
      (:color nest))))

(defn cell [x y]
  (let [cell-atom (reagent/cursor cells [[x y]])
        dist (Math/sqrt (+ (* x x) (* y y)))]
    (fn [& _]
      (let [color (color-for @cell-atom)]
        [:div.cell {:class (if (> dist 25.5) "perimeter" "")
                    :style (when color {:background-color color})}])
      )))

(defn log-panel []
  [:div.logs
   (for [[key log] @logs]
     [:span.log {:key key} log])])

(defn nest [nest-atom]
  [:div.nest
   [:div.swatch {:style {:background-color (:color @nest-atom)}}]
   [:span (:team @nest-atom)]
   [:span.score (str (:ants @nest-atom) "/" (:food @nest-atom))]])

(defn nests-panel []
  [:div.nests
   [:div.nest
    [:div.swatch {:style {:background-color (:color "transparent")}}]
    [:span [:i "Team Name"]]
    [:span.score [:i "Ants/Food"]]]
   (for [anest (sort-by :team (vals @nests))]
     (with-meta
       [nest (reagent/cursor nests [(:id anest)])]
       {:key (:id anest)}))])

(def visible-coords (for [y (range -25 26) x (range -25 26)] (list x y)))

(defn add-food [e]
  (remote/call! :arena/add-food {:location (rand-nth visible-coords)}))

(defn add-new-logs [old new]
  (concat (reverse (map #(list (.random js/Math) %) new)) old))

(defn reset-world [e]
  (swap! logs add-new-logs ["Resetting World!"])
  (remote/call! :arena/reset-world {}
                :on-success (fn [_]
                              (reset! state start-state)
                              (swap! logs add-new-logs ["World reset!"]))))

(defn arena []
  [:div.arena
   [:div.top-panel
    [:h1 "Ant Sparring"]]
   [:div.middle
    [:div.left-panel
     [:h2 "Nests"]
     [nests-panel]]
    [:div.world
     (for [[x y] visible-coords]
       (with-meta [cell x y] {:key (str x ", " y)}))]
    [:div.right-panel
     [:h2 "Log"]
     [log-panel]]]
   [:div.bottom-panel
    [:button {:on-click add-food} "Add Food"]
    [:button {:on-click #(remote/call! :arena/clear-food {})} "Clear Food"]
    [:button {:on-click reset-world} "Reset World"]]]
  )

(defn ^:export init [payload-src]
  (log/off!)
  (let [payload (transit/read transit-reader payload-src)
        body (.-body js/document)]
    (reagent/render-component [arena payload] body)
    (log/info "connecting!")
    (remote/connect!))
  ;(swap! logs conj [123 "This is a test"])
  )

(defn- assign-color [nest]
  (if (:color nest)
    nest
    (let [color (first @colors)]
      (swap! colors rest)
      (assoc nest :color color))))

(defn compile-nests [nests world]
  (let [things (vals world)
        all (filter #(= :nest (:type %)) things)]
    (reduce (fn [nests nest]
              (if-let [existing (get nests (:id nest))]
                (assoc nests (:id nest) (merge existing nest))
                (assoc nests (:id nest) (assign-color nest))))
            nests all)))

(defn prioritize-thing [old new]
  (cond
    (nil? old) new
    (nest? old) old
    (food? old) old
    :else new))

(defn update-cells [cells world]
  (reduce
    (fn [result thing]
      (let [loc (:location thing)
            old (get result loc)]
        (assoc result loc (prioritize-thing old thing))))
    {} (vals world)))

(defmethod remote/push-event-handler :ants/update [[_ [world new-logs]]]
  ;(log/info "world new-logs: " world new-logs)
  (swap! logs add-new-logs new-logs)
  (swap! nests compile-nests world)
  (swap! cells update-cells world)
  )