(defproject ants/api "0.0.1"
  :description "Web platform/api for Ants Sparring"
  :dependencies [[com.cognitect/transit-clj "0.8.271" :exclusions [commons-codec]]
                 [com.taoensso/timbre "4.1.2"]
                 [joodo "2.1.0" :exclusions [com.taoensso/timbre hiccup]]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.7.0"]
                 [pandeiro/http-kit "2.1.20-SNAPSHOT"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [ring-server/ring-server "0.3.1" :exclusions [ring-refresh]]]
  :profiles {:dev {:repl-options {:init-ns ants.repl
                                  :timeout 120000}
                   :dependencies [[cljsjs/react-with-addons "0.13.3-0"]
                                  [com.cognitect/transit-cljs "0.8.225"]
                                  [com.taoensso/sente "1.7.0-RC1"]
                                  [org.clojure/clojurescript "1.7.48" :exclusions [org.clojure/tools.reader]]
                                  [reagent "0.5.1" :exclusions [org.clojure/clojurescript cljsjs/react]]
                                  [speclj "3.3.1"]]}}
  :plugins [[speclj "3.3.1"]
            [lein-pdo "0.1.1"]]
  :test-paths ["spec"]
  :source-paths ["src"]
  :main ants.api
  :clean-targets ^{:protect false} [:target-path "resources/public/cljs"]
  :aliases {"cljs"    ["do" "clean," "run" "-m" "ants.cljs" "once"]
            "specljs" ["do" "clean," "run" "-m" "ants.cljs" "once" "development"]
            "dev"     ["pdo" "clean," "run" "-m" "ants.cljs" "auto"]})