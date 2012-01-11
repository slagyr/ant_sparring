(ns ants.ui.core
  (:import
    [java.util.concurrent TimeUnit ScheduledThreadPoolExecutor]))

(defprotocol AntsUI
  (update-ui [this data]))

(defprotocol AntsDataSource
  (curl [this url]))

(defprotocol AntsInteractor
  (startup [this host])
  (shutdown [this])
  (update [this]))

(defn- do-curl [url-str]
  (let [url (java.net.URL. url-str)
        input (.openStream url)
        reader (java.io.InputStreamReader. input)
        pushback (java.io.PushbackReader. reader)]
    (try
      (read pushback)
      (finally (.close pushback)))))

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
