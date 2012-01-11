(ns ants.api.core
  (:use
    [compojure.core :only (defroutes GET)]
    [compojure.route :only (not-found)]
    [joodo.env :only (development-env?)]
    [joodo.middleware.verbose :only (wrap-verbose)]
    [ants.engine.core]))

(defn text [& message]
  {:status 200 :content-type "text/plain" :body (apply str message)})

(defn- marshal [value]
  (with-out-str (prn value)))

(defmacro do-cmd [call]
  `(try
     (let [id# ~call]
       (locking *world* (.wait *world* TICK-DURATION))
       (marshal {:response "ok" :stat (stat *world* id#)}))
     (catch Exception ~'e
       (marshal {:response "error" :message (.getMessage ~'e)}))))

(def DOC
  "Commands:
    /join/:name           Join the arena.  Your name must be unique.
    /:id/look             Look around. See what's up.
    /:id/go/:direction    Move either north, east, south, or west"
    )

(defroutes api-routes
  (GET "/" [] (text "Welcome to Ant Sparring!" "\n\n" DOC))
  (GET "/_admin_/start" [] (do (start *world*) (text "The world has started")))
  (GET "/_admin_/stop" [] (do (stop *world*) (text "The world has stopped")))
  (GET "/_admin_/feed" [] (marshal (get-feed *world*)))
  (GET "/_admin_/place-food/:x/:y" {{x :x y :y} :params} (marshal (place-food *world* [(Integer/parseInt x) (Integer/parseInt y)])))
  (GET "/_admin_/remove-food/:x/:y" {{x :x y :y} :params} (marshal (remove-food *world* [(Integer/parseInt x) (Integer/parseInt y)])))
  (GET "/join/:name" {{name :name} :params} (do-cmd (join *world* name)))
  (GET "/:id/look" {{id :id} :params} (do-cmd (look *world* id)))
  (GET "/:id/go/:direction" {{id :id dir :direction} :params} (do-cmd (go *world* id dir)))
  (not-found "Are you lost little ant?"))

(def joodo-handler
  (if (development-env?)
    (wrap-verbose api-routes)
    api-routes))
