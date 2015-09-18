(ns ants.view.arena
  (:require [cognitect.transit :as transit]
            [hiccup.core :as hiccup]
            [hiccup.element :as elem]
            [hiccup.page :as page]
            [joodo.env :as env]
            [joodo.middleware.asset-fingerprint :refer [add-fingerprint]]
            [joodo.middleware.request :refer [*request*]]
            [ring.util.response :as response])
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
          (if env/development?
            (list
              (page/include-js "/cljs/react-with-addons.inc.js")
              (page/include-js "/cljs/goog/base.js")
              (page/include-js "/cljs/ants_dev.js"))
            (page/include-js (add-fingerprint "/cljs/ants.js")))
          (elem/javascript-tag (str "goog.require('ants.view.arena');"))]
         [:body (str "<script type=\"text/javascript\">"
                     "//<![CDATA[\n"
                     (when (env/development?)
                       "goog.require('ants.view.development');\n")
                     "ants.view.arena.init(" (pr-str (->transit {})) ");\n"
                     "//]]></script>")]]))))