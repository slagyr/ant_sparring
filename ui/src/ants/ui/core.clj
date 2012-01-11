(ns ants.ui.core
  (:use
    [limelight.clojure.core]
    [limelight.clojure.prop-building])
  (:import
    [java.util.concurrent TimeUnit ScheduledThreadPoolExecutor]))

; UI ------------------------------------------------------------

(defprotocol AntsUI
  (update-ui [this data]))

(defn- translated-location [thing]
  (let [[x y] (:location thing)]
    {:x (+ 300 (* 60 x)) :y (+ 300 (* 60 y))}))

(defn- options-for [thing]
  (let [options (translated-location thing)]
    (if-let [name (:name thing)]
      (assoc options :text name :background-color (:color thing))
      options)))

(defn place-stuff [scene stuff]
  (let [world (find-by-id scene "world")]
    (.removeAll (.getPeer world))
    (build-props world
      (vec
        (for [thing stuff]
          [(:type thing) (options-for thing)])))))

(defn place-scores [scene scores]
  (let [scores-prop (find-by-id scene "scores")]
    (.removeAll (.getPeer scores-prop))
    (build-props scores-prop
      (vec
        (for [score scores]
          [:score {:background-color (:color score)}
           [:score-name {:text (:name score)}]
           [:score-value {:text (str (:points score))}]])))))

(defn place-logs [scene logs]
  (let [log-prop (find-by-id scene "log")]
    (.removeAll (.getPeer log-prop))
    (build-props log-prop
      (vec
        (for [entry logs]
          [:log-entry {:text entry}])))))

(deftype LimelightAntsUI [production]
  AntsUI
  (update-ui [this data]
    (let [theater (theater production)
          stage (get-stage theater "arena")
          scene (scene stage)]
      (place-stuff scene (:stuff data))
      (place-scores scene (:scores data))
      (place-logs scene (:log data)))))

(defn new-ants-ui [production]
  (LimelightAntsUI. production))

; DATA_SOURCE ---------------------------------------------------

(defprotocol AntsDataSource
  (curl [this url]))

(defn- do-curl [url-str]
  (let [url (java.net.URL. url-str)
        input (.openStream url)
        reader (java.io.InputStreamReader. input)
        pushback (java.io.PushbackReader. reader)]
    (try
      (read pushback)
      (finally (.close pushback)))))

(deftype RealAntsDataSource []
  AntsDataSource
  (curl [this url] (do-curl url)))

(defn new-ants-data-source []
  (RealAntsDataSource.))

; INTERACTOR -----------------------------------------------------

(defprotocol AntsInteractor
  (startup [this host])
  (shutdown [this])
  (update [this]))


(def COLORS
  ["alice_blue"
   "antique_white"
   "aqua"
   "aquamarine"
   "azure"
   "beige"
   "bisque"
   "black"
   "blanched_almond"
   "blue"
   "blue_violet"
   "brown"
   "burly_wood"
   "cadet_blue"
   "chartreuse"
   "chocolate"
   "coral"
   "cornflower_blue"
   "cornsilk"
   "crimson"
   "cyan"
   "dark_blue"
   "dark_cyan"
   "dark_golden_rod"
   "dark_gray"
   "dark_green"
   "dark_khaki"
   "dark_magenta"
   "dark_olive_green"
   "darkorange"
   "dark_orchid"
   "dark_red"
   "dark_salmon"
   "dark_sea_green"
   "dark_slate_blue"
   ])

(defn- rand-color []
  (get COLORS (int (rand (count COLORS)))))

(defn- assign-color [colors thing]
  (if (and (= :ant (:type thing)) (nil? (:color thing)))
    (let [color (rand-color)]
      (alter colors assoc (:id thing) color)
      (assoc thing :color color))
    thing))

(defn- assign-colors [colors stuff]
  (map #(assign-color colors %) stuff))

(defn- thing-sorter [a b]
  (.compareTo (name (:id a)) (name (:id b))))

(defn- point-sorter [a b]
  (.compareTo (or (:points a) 0) (or (:points b) 0)))

(defn build-stuff [interactor stuff]
  (dosync
    (let [things (assign-colors (.colors interactor) (vals stuff))]
      (sort thing-sorter things))))

(defn- build-scores [stuff]
  (take 10
    (reverse
      (sort point-sorter
        (filter #(= :ant (:type %)) stuff)))))

(defn- build-log [interactor log]
  (dosync
    (let [result (take 50 (concat (reverse log) @(.log interactor)))]
      (ref-set (.log interactor) result)
      result)))

(deftype RealAntsInteractor [ui ds host scheduler colors log]
  AntsInteractor
  (startup [this host]
    (reset! (.host this) host)
    (curl ds (str host "/_admin_/start"))
    (reset! scheduler (ScheduledThreadPoolExecutor. 1))
    (.scheduleWithFixedDelay @scheduler #(update this) 0 1 TimeUnit/SECONDS))
  (shutdown [this]
    (when @scheduler
      (.shutdown @scheduler)
      (reset! scheduler nil)
      (curl ds (str @host "/_admin_/stop"))))
  (update [this]
    (let [feed (curl ds (str @host "/_admin_/feed"))
          stuff (build-stuff this (:stuff feed))
          scores (build-scores stuff)
          log (build-log this (:log feed))]
      (update-ui ui {:stuff stuff :scores scores :log log}))))


(defn new-ants-interactor [ui ds]
  (ants.ui.core.RealAntsInteractor. ui ds (atom nil) (atom nil) (ref {}) (ref {})))
