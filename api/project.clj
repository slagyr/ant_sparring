(defproject ants/api "0.0.1"
            :description "Web platform/api for Ants Sparring"
            :dependencies [[org.clojure/clojure "1.7.0"]
                           [joodo "2.1.0"]
                           [pandeiro/http-kit "2.1.20-SNAPSHOT"]
                           [ants/engine "1.0.0"]]
            :profiles {:dev {:dependencies [[speclj "3.3.1"]]}}
            :plugins [[speclj "3.3.1"]]
            :test-paths ["spec"]
            :source-paths ["src"]
            :main ants.api.core)