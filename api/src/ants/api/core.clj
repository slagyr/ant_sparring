(ns ants.api.core
  (:use
    [compojure.core :only (defroutes GET)]
    [compojure.route :only (not-found)]
    [joodo.env :only (development-env?)]
    [joodo.middleware.verbose :only (wrap-verbose)]
;    [joodo.middleware.view-context :only (wrap-view-context)]
;    [joodo.views :only (render-template render-html)]
;    [joodo.controllers :only (controller-router)]
    ))

(defroutes api-routes
  (GET "/" [] {:status 200 :content-type "text/plain" :body "Welcome to Ant Sparring!"})
;  (controller-router 'ants.api.controller)
  (not-found "Are you lost little ant?"))

(def joodo-handler
  (if (development-env?)
    (wrap-verbose api-routes)
    api-routes))

;(def app-handler
;  (->
;    api-routes
;    (wrap-view-context :template-root "ants/api/view" :ns `ants.api.view.view-helpers)))
