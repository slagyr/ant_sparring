(ns ants.main
  (:require [ants.app :as app]
            [ants.engine :as engine]
            [ants.init :as init]
            [ants.log :as log]
            [org.httpkit.server :refer [run-server]]
            [ants.util :as util]
            [ants.arena :as arena]))

(defn -main [& args]
  (when-let [arg (first args)]
    (swap! (app/app) assoc :env arg))
  (when (app/dev?)
    (let [init-refresh (util/resolve-var 'ants.refresh/init)]
      (init-refresh 'ants.init/stop 'ants.init/start)))
  (let [world (engine/new-world arena/world-updated)]
    (swap! (app/app) assoc :world world))
  (init/start)
  (let [port (if-let [port-str (System/getenv "PORT")] (Integer/parseInt port-str) 8888)]
    (log/info (str "Starting server on port " port))
    (run-server @(util/resolve-var 'ants.api/app) {:port port})))