(ns ants.api.core
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [not-found]]
            [joodo.env :as env]
            [joodo.middleware.verbose :refer [wrap-verbose]]
            [ants.engine.core :as ant]))

(defn- text [& message]
  {:status 200 :content-type "text/plain" :body (str "\"" (apply str message) "\"")})

(defn- marshal [status value]
  {:status       status
   :content-type "text/plain"
   :body         (with-out-str (prn value))})

(defmacro do-cmd [call]
  `(try
     (let [id# ~call]
       (locking ant/*world* (.wait ant/*world* ant/TICK-DURATION))
       (marshal 200 {:response "ok" :stat (ant/stat ant/*world* id#)}))
     (catch Exception ~'e
       (marshal 500 {:response "error" :message (.getMessage ~'e)}))))

(def DOC
  "Commands:
    /join/:name           Join the arena.  Your name must be unique.
    /:id/look             Look around. See what's up.
    /:id/go/:direction    Move either north, east, south, or west"
  )

(defroutes api-routes
  (GET "/" [] (text "Welcome to Ant Sparring!" "\n\n" DOC))
  (GET "/_admin_/start" [] (do (ant/start ant/*world*) (text "The world has started")))
  (GET "/_admin_/stop" [] (do (ant/stop ant/*world*) (text "The world has stopped")))
  (GET "/_admin_/feed" [] (marshal 200 (ant/get-feed ant/*world*)))
  (GET "/_admin_/place-food/:x/:y" {{x :x y :y} :params} (marshal 200 (ant/place-food ant/*world* [(Integer/parseInt x) (Integer/parseInt y)])))
  (GET "/_admin_/remove-food/:x/:y" {{x :x y :y} :params} (marshal 200 (ant/remove-food ant/*world* [(Integer/parseInt x) (Integer/parseInt y)])))
  (GET "/join/:name" {{name :name} :params} (do-cmd (ant/join ant/*world* name)))
  (GET "/:id/look" {{id :id} :params} (do-cmd (ant/look ant/*world* id)))
  (GET "/:id/go/:direction" {{id :id dir :direction} :params} (do-cmd (ant/go ant/*world* id dir)))
  (not-found "Are you lost little ant?"))

(def joodo-handler
  (if (env/development?)
    (wrap-verbose api-routes)
    api-routes))
