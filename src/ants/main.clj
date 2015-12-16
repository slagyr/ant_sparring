(ns ants.main
  (:require [ants.api :as api]
            [ants.app :as app]
            [ants.engine :as engine]
            [ants.init :as init]
            [ants.log :as log]
            [ants.remote :as remote]
            [org.httpkit.server :refer [run-server]]
            [joodo.env :as env]
            [ants.util :as util]))

(defn -main []
  (when (env/development?)
    (let [init-refresh (util/resolve-var 'ants.refresh/init)]
      (init-refresh 'ants.init/stop 'ants.init/start)))
  (let [world (engine/new-world remote/world-updated)]
    (swap! (app/app) assoc :world world))
  (init/start)
  (let [port (if-let [port-str (System/getenv "PORT")] (Integer/parseInt port-str) 8888)]
    (log/info (str "Starting server on port " port))
    (run-server api/app {:port port})))