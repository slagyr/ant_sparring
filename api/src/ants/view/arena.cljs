(ns ants.view.arena
  (:require [ants.view.remote :as remote]
            [cognitect.transit :as transit]
            [goog.events]
            [reagent.core :as reagent]))

(defonce state (reagent/atom {}))

(def transit-reader (transit/reader :json))

(defn arena []
  [:h1 "World"]
  )

(defn ^:export init [payload-src]
  (let [payload (transit/read transit-reader payload-src)
        body (.-body js/document)]
    (reagent/render-component [arena payload] body)
    (remote/connect!)))