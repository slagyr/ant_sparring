(ns ants.init
  (:require [ants.app :as app]
            [ants.engine :as engine]
            [ants.log :as log]
            [ants.remote :as remote]
            [joodo.env :as env]))

(defn start-services [app]
  (log/info "Starting services")
  (-> app
      remote/start
      engine/start))

(defn stop-services [app]
  (log/info "Stopping services")
  (-> app
      engine/stop
      remote/stop))

(defn start []
  (log/with-log-level
    :info
    (log/info "Airworthy Environment: " (env/env :joodo-env))
    (log/info "before starting app/app: " (keys @(app/app)))
    (swap! (app/app) start-services)
    (log/info "after startin app/app: " (keys @(app/app)))))

(defn stop []
  (swap! (app/app) stop-services)
  (log/info "after stopping app/app: " (keys @(app/app))))