(ns ants.log
  (:require [clojure.string :as str]
            [taoensso.encore :as enc]))

(defn trace [& args] (enc/tracef (str/join " " args)))
(defn debug [& args] (enc/debugf (str/join " " args)))
(defn info [& args] (enc/infof (str/join " " args)))
(defn warn [& args] (enc/warnf (str/join " " args)))
(defn error [& args] (enc/errorf (str/join " " args)))
(defn fatal [& args] (enc/fatalf (str/join " " args)))
(defn report [& args] (enc/reportf (str/join " " args)))

(defn ^:export off! [] (set! enc/*log-level* :report))
(defn ^:export error! [] (set! enc/*log-level* :error))
(defn ^:export warn! [] (set! enc/*log-level* :warn))
(defn ^:export info! [] (set! enc/*log-level* :info))
(defn ^:export debug! [] (set! enc/*log-level* :debug))
(defn ^:export all! [] (set! enc/*log-level* :trace))

(off!)