(ns ants.arena
  (:require [ants.app :as app]
            [ants.engine :as engine]
            [ants.remote :as remote]
            [cognitect.transit :as transit]
            [hiccup.core :as hiccup]
            [hiccup.element :as elem]
            [hiccup.page :as page]
            [joodo.middleware.asset-fingerprint :refer [add-fingerprint]]
            [joodo.middleware.request :refer [*request*]]
            [ring.util.response :as response]
            [ants.log :as log])
  (:import (java.io ByteArrayOutputStream)))

(defn ->transit [data]
  (let [baos (ByteArrayOutputStream.)
        writer (transit/writer baos :json)]
    (transit/write writer data)
    (.close baos)
    (.toString baos)))

(defn html-response []
  (response/response
    (hiccup/html
      (list
        (page/doctype :html5)
        [:html {}
         [:head {}
          [:meta {:http-equiv "Content-Type" :content "text/html" :charset "UTF-8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
          [:title "Ants"]
          (page/include-css (add-fingerprint "/stylesheets/ants.css"))
          (if true ;(app/dev?)
            (list
              (page/include-js "/cljs/react-with-addons.inc.js")
              (page/include-js "/cljs/goog/base.js")
              (page/include-js "/cljs/ants_dev.js"))
            (page/include-js (add-fingerprint "/cljs/ants.js")))
          (elem/javascript-tag (str "goog.require('ants.arena');"))]
         [:body (str "<script type=\"text/javascript\">"
                     "//<![CDATA[\n"
                     (when (app/dev?)
                       "goog.require('ants.development');\n")
                     "ants.arena.init(" (pr-str (->transit {})) ");\n"
                     "//]]></script>")]]))))

(defn world-updated [stuff last-logs]
  (try
    ;(log/info "stuff last-logs: " stuff (pr-str last-logs))
    ;(prn "@connected-uids: " @connected-uids)
    (doseq [uid (:any @@remote/connected-uids)]
      ;(prn "uid: " uid)
      (@remote/send-fn uid [:ants/update [stuff last-logs]]))
    (catch Exception e (log/error e))))

(defn ^:remote add-food [{:keys [location]}]
  (remote/success
    (engine/place-food @app/world location)))

(defn ^:remote clear-food [_]
  (remote/success
    (engine/remove-all-food @app/world)))

(defn ^:remote reset-world [_]
  (swap! (app/app) engine/stop)
  (let [world (engine/new-world world-updated)]
    (swap! (app/app) assoc :world world)
    (swap! (app/app) engine/start)
    (remote/success {})))

(defn ^:remote toggle-food [{:keys [location]}]
  (if (engine/food-at? @(.stuff @app/world) location)
    (engine/remove-food @app/world location)
    (engine/place-food @app/world location))
  (remote/success "Food toggled"))