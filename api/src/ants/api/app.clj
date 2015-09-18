(ns ants.api.app
  (:import (clojure.lang IDeref)))

(def app {})

(defn resolution [key]
  (if (vector? key)
    (reify IDeref
      (deref [_]
        (if-let [value (get-in app key)]
          value
          (throw (Exception. (str "Unresolved app component: " key))))))
    (reify IDeref
      (deref [_]
        (if-let [value (get app key)]
          value
          (throw (Exception. (str "Unresolved app component: " key))))))))

