(defproject ants/api "0.0.1"
            :description "Web platform/api for Ants Sparring"
            :dependencies [[joodo "2.1.0"]
                           [org.clojure/clojure "1.7.0"]
                           [pandeiro/http-kit "2.1.20-SNAPSHOT"]
                           [ring-server/ring-server "0.3.1" :exclusions [ring-refresh]]]
            :profiles {:dev {:repl-options {:init-ns ants.repl
                                              :timeout 120000}
                             :dependencies [[speclj "3.3.1"]]}}
            :plugins [[speclj "3.3.1"]]
            :test-paths ["spec"]
            :source-paths ["src"]
            :main ants.api)