(defproject ants/api "0.0.1"
  :description "Web platform/api for Ants Sparring"
  :dependencies [[ants/engine "1.0.0"]
                 [com.cognitect/transit-clj "0.8.271" :exclusions [commons-codec]]
                 [com.taoensso/timbre "4.0.2"]
                 [joodo "2.1.0" :exclusions [com.taoensso/timbre hiccup]]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.7.0"]
                 [pandeiro/http-kit "2.1.20-SNAPSHOT"]
                 [ring-server/ring-server "0.3.1" :exclusions [ring-refresh]]]
  :profiles {:dev
             {:dependencies [
                             ;[alandipert/storage-atom "1.2.4"]
                             ;[cljsjs/hashids "1.0.2-0"]
                             [cljsjs/react-with-addons "0.12.2-7"]
                             ;[com.andrewmcveigh/cljs-time "0.3.4"]
                             [com.cognitect/transit-cljs "0.8.220"]
                             [com.taoensso/sente "1.5.0"]
                             ;[garden "1.2.1"]
                             [org.clojure/clojurescript "0.0-3308"]
                             ;[org.clojure/tools.namespace "0.2.11"]
                             [reagent "0.5.0" :exclusions [org.clojure/clojurescript cljsjs/react]]
                             [speclj "3.3.1"]]}}
  :plugins [[speclj "3.3.1"]
            [lein-pdo "0.1.1"]]
  :test-paths ["spec"]
  :source-paths ["src"]
  :main ants.api.core

  :aliases {"cljs"    ["do" "clean," "run" "-m" "ants.cljs" "once"]
            "specljs" ["do" "clean," "run" "-m" "ants.cljs" "once" "development"]
            "dev"     ["pdo" "clean," "run" "-m" "ants.cljs" "auto"]}
  )