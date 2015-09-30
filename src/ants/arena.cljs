(ns ants.arena
  (:require [ants.remote :as remote]
            [cognitect.transit :as transit]
            [goog.events]
            [reagent.core :as reagent]))

(defonce state (reagent/atom {}))

(def transit-reader (transit/reader :json))

(defn arena []
  [:div.arena
   [:div.top-panel
    [:h1 "Ant Sparring"]]
   [:div.middle
    [:div.left-panel]
    [:div.world]
    [:div.right-panel]]
   [:div.bottom-panel]]
  )

(defn ^:export init [payload-src]
  (let [payload (transit/read transit-reader payload-src)
        body (.-body js/document)]
    (reagent/render-component [arena payload] body)
    (remote/connect!)))