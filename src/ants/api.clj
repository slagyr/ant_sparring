(ns ants.api
  (:require [ants.app :as app]
            [ants.arena :as arena]
            [ants.engine :as engine]
            [ants.log :as log]
            [ants.util :as util]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [not-found]]
            [joodo.env :as env]
            [joodo.middleware.request :refer [wrap-bind-request *request*]]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.session :refer [wrap-session]]))

(defn- text [& message]
  {:status 200 :content-type "text/plain" :body (apply str message)})

(defn- marshal [status value]
  {:status       status
   :content-type "text/plain"
   :body         (with-out-str (prn value))})

(defn do-cmd [cmd]
  (try
    (let [id (cmd)]
      (locking @app/world (.wait @app/world (* 2 engine/TICK-DURATION)))
      (prn "engine/*world*: " (.stuff @app/world))
      (marshal 200 {:response "ok" :stat (engine/stat @app/world id)}))
    (catch Exception e
      (log/error e)
      (marshal 500 {:response "error" :message (.getMessage e)}))))

(def DOC
  "Commands:
    /join/:name               Join the arena.  Your (team) name must be unique.  Returns the id of your new-born nest.
    /:nest-id/spawn           Spawns and ant.  Requires 1 food.  Returns the id of your new ant.
    /:ant-id/look             Look around. See what's up.
    /:ant-id/go/:direction    Move either n, ne, e, se, s, sw, w, nw
    /:id/stat                 Immediately returns the status of the object with :id"
  )


(defroutes handler
  (GET "/" [] (text "Welcome to Ant Sparring!" "\n\n" DOC))
  (GET "/arena" [] (arena/html-response))
  (GET "/_admin_/start" [] (do (engine/start @app/world) (text "The world has started")))
  (GET "/_admin_/stop" [] (do (engine/stop @app/world) (text "The world has stopped")))
  (GET "/_admin_/feed" [] (marshal 200 (engine/get-feed @app/world)))
  (GET "/_admin_/place-food/:x/:y" {{x :x y :y} :params} (marshal 200 (engine/place-food @app/world [(Integer/parseInt x) (Integer/parseInt y)])))
  (GET "/_admin_/remove-food/:x/:y" {{x :x y :y} :params} (marshal 200 (engine/remove-food @app/world [(Integer/parseInt x) (Integer/parseInt y)])))
  (GET "/join/:name" {{name :name} :params} (do-cmd #(engine/join @app/world name)))
  (GET "/:id/look" {{id :id} :params} (do-cmd #(engine/look @app/world id)))
  (GET "/:id/go/:direction" {{id :id dir :direction} :params} (do-cmd #(engine/go @app/world id dir)))
  (GET "/:id/spawn" {{id :id dir :direction} :params} (do-cmd #(engine/spawn @app/world id)))
  (GET "/:id/stat" {{id :id} :params} (marshal 200 {:response "ok" :stat (engine/stat @app/world id)})))

(defn wrap-dev-maybe [handler]
  (if (env/development?)
    (let [wrapper (util/resolve-var 'ants.refresh/wrap-development)]
      (wrapper handler))
    handler))

(defn refreshable [handler-sym]
  (if (env/development?)
    (fn [request] (@(util/resolve-var handler-sym) request))
    (util/resolve-var handler-sym)))

(defroutes web-routes
  (refreshable 'ants.remote/handler)
  (refreshable 'ants.api/handler)
  (not-found "Are you lost little ant?"))

(def app
  (-> web-routes
      ;wrap-dev-maybe
      wrap-bind-request
      wrap-anti-forgery
      wrap-keyword-params
      wrap-params
      wrap-session
      (wrap-resource "public")
      wrap-file-info))