(defproject ants/engine "1.0.0"
            :description "Engine for Ants Sparring"
            :dependencies [[org.clojure/clojure "1.7.0"]]
            :profiles {:dev {:dependencies [[speclj "3.3.1"]]}}
            :plugins [[speclj "3.3.1"]]
            :test-paths ["spec"]
            :source-paths ["src"])