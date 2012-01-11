(defproject ants/api "0.0.1"
  :description "A website deployable to AppEngine"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [joodo "0.6.0-SNAPSHOT"]
                 [ants/engine "1.0.0"]]
  :dev-dependencies [[speclj "2.0.1"]]
  :test-path "spec/"
  :java-source-path "src/"
  :repl-init-script "config/development/repl_init.clj"
  :joodo-core-namespace ants.api.core)