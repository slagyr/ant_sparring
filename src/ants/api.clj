(ns ants.api
  (:require [ants.engine :as ant]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [not-found]]
            [joodo.middleware.request :refer [wrap-bind-request *request*]]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [org.httpkit.server :refer [run-server]]))

(defn- text [& message]
  {:status 200 :content-type "text/plain" :body (apply str message)})

(defn- marshal [status value]
  {:status       status
   :content-type "text/plain"
   :body         (with-out-str (prn value))})

(defn do-cmd [cmd]
  (try
    (let [id (cmd)]
      (locking ant/*world* (.wait ant/*world* (* 2 ant/TICK-DURATION)))
      (marshal 200 {:response "ok" :stat (ant/stat ant/*world* id)}))
    (catch Exception e
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
  (GET "/_admin_/start" [] (do (ant/start ant/*world*) (text "The world has started")))
  (GET "/_admin_/stop" [] (do (ant/stop ant/*world*) (text "The world has stopped")))
  (GET "/_admin_/feed" [] (marshal 200 (ant/get-feed ant/*world*)))
  (GET "/_admin_/place-food/:x/:y" {{x :x y :y} :params} (marshal 200 (ant/place-food ant/*world* [(Integer/parseInt x) (Integer/parseInt y)])))
  (GET "/_admin_/remove-food/:x/:y" {{x :x y :y} :params} (marshal 200 (ant/remove-food ant/*world* [(Integer/parseInt x) (Integer/parseInt y)])))
  (GET "/join/:name" {{name :name} :params} (do-cmd #(ant/join ant/*world* name)))
  (GET "/:id/look" {{id :id} :params} (do-cmd #(ant/look ant/*world* id)))
  (GET "/:id/go/:direction" {{id :id dir :direction} :params} (do-cmd #(ant/go ant/*world* id dir)))
  (GET "/:id/spawn" {{id :id dir :direction} :params} (do-cmd #(ant/spawn ant/*world* id)))
  (GET "/:id/stat" {{id :id} :params} (marshal 200 {:response "ok" :stat (ant/stat ant/*world* id)}))
  (not-found "Are you lost little ant?"))

(def app
  (-> handler
      wrap-bind-request
      wrap-keyword-params
      wrap-params))

(defn server [world]
  (ant/start world)
  (run-server app {:port 8888}))

(defn -main [] (server (ant/new-world)))