(ns ants.cljs
  (:require [cljs.build.api :as api]
            [clojure.java.io :as io])
  (:import (cljs.closure Inputs Compilable)))

(defn run-specs [auto?]
  (let [cmd (str "phantomjs resources/public/specs/speclj.js" (when auto? " auto"))
        process (.exec (Runtime/getRuntime) cmd)
        output (.getInputStream process)
        error (.getErrorStream process)]
    (io/copy output (System/out))
    (io/copy error (System/err))
    (when-not auto?
      (System/exit (.waitFor process)))))

(def options
  {:development {:optimizations  :none
                 :output-to      "resources/public/cljs/ants_dev.js"
                 :output-dir     "resources/public/cljs/"
                 :cache-analysis true
                 :source-map     true
                 :pretty-print   true
                 :verbose        false
                 :watch-fn       #(run-specs true)
                 :specs          true
                 :sources        ["spec" "src"]}
   :production  {:optimizations  :advanced
                 :output-to      "resources/public/cljs/ants.js"
                 :output-dir     "resources/public/cljs/"
                 :cache-analysis false
                 :pretty-print   false
                 :verbose        false
                 :specs          false
                 :sources        ["src"]}
   :staging     {:optimizations  :advanced
                 :output-to      "resources/public/cljs/ants.js"
                 :output-dir     "resources/public/cljs/"
                 :pseudo-names   true
                 :cache-analysis false
                 :pretty-print   true
                 :verbose        false
                 :specs          false
                 :sources        ["src"]}})

(deftype Sources [build-options]
  Inputs
  (-paths [_] (map io/file (:sources build-options)))
  Compilable
  (-compile [_ opts] (mapcat #(cljs.closure/compile-dir (io/file %) opts) (:sources build-options))))

(defn establish-path [path]
  (let [file (io/file path)
        parent (.getParentFile file)]
    (when (not (.exists parent))
      (.mkdirs parent))))

(defn -main [once-or-auto & args]
  (let [build (keyword (or (first args) (System/getenv "JOODO_ENV") "development"))
        build-options (get options build)]
    (assert (#{"once" "auto"} once-or-auto) (str "Unrecognized build frequency: " once-or-auto ". Must be 'once' or 'auto'"))
    (println "Compiling ClojureScript:" once-or-auto build)
    (establish-path (:output-to build-options))
    (if (= "once" once-or-auto)
      (do
        (api/build (Sources. build-options) build-options)
        (when (:specs build-options)
          (run-specs false)))
      (api/watch (Sources. build-options) build-options))))