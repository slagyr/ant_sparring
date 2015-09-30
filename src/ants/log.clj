(ns ants.log
  (:require [clojure.string :as str]
            [chee.pretty-map :as pretty-map]
            [taoensso.timbre :as timbre]))

(defmacro trace [& args] `(timbre/trace ~@args))
(defmacro debug [& args] `(timbre/debug ~@args))
(defmacro info [& args] `(timbre/info ~@args))
(defmacro warn [& args] `(timbre/warn ~@args))
(defmacro error [& args] `(timbre/error ~@args))
(defmacro fatal [& args] `(timbre/fatal ~@args))
(defmacro report [& args] `(timbre/report ~@args))

(defn set-level! [level]`
  (report (str "Setting log level: " level))
  (timbre/set-level! level))

(defn off! [] (set-level! :report))
(defn error! [] (set-level! :error))
(defn warn! [] (set-level! :warn))
(defn info! [] (set-level! :info))
(defn all! [] (set-level! :trace))

(def captured-logs (atom []))

(defmacro with-log-level [level & body] `(timbre/with-log-level ~level ~@body))

(defmacro capture-logs [& body]
  `(let [original-level# (:level timbre/*config*)]
     (reset! captured-logs [])
     (try
       (timbre/set-level! :trace)
       (with-redefs [timbre/log1-fn (fn [& args#] (swap! captured-logs conj args#))]
         ~@body)
     (finally
       (timbre/set-level! original-level#)))))

(defn parse-captured-logs []
  (map
    #(fn [[config level ?ns-str ?file ?line msg-type vargs_ ?base-data]]
      {:level level :message (apply str @vargs_)})
    @captured-logs))

(defn captured-logs-str []
  (str/join "\n" (map #(str/join " " %) (map #(deref (nth % 6)) @captured-logs))))

(defn pretty-map [m] (pretty-map/pretty-map m))

(defn table-spec [& cols]
  (let [width (+ (apply + (map second cols)) (count cols))
        format-str (str/join " " (map #(str "%-" (second %) "s") cols))]
    {:cols     cols
     :format   format-str
     :width    width
     :title-fn (fn [title]
                 (let [pad (/ (- width (.length title)) 2)]
                   (str (str/join "" (take pad (repeat " "))) title "\n")))
     :header   (str (apply (partial format format-str) (map first cols)) "\n"
                    (str/join "" (take width (repeat "-"))) "\n")
     }))

(defn color-pr
  "For ANSI color codes: https://en.wikipedia.org/wiki/ANSI_escape_code"
  [message color]
  (println (str "\u001b[" color "m" message "\u001b[0m")))