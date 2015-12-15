(ns ants.app
  (:import (clojure.lang IDeref)))

; START HACK --------------------
(ns ants.app.static
  (:import (clojure.lang IDeref)))

(when-not (bound? (resolve 'app))
  (def app (atom {})))

(in-ns 'ants.app)
; END HACK ----------------------

(defn app [] ants.app.static/app)

(defn resolution [key]
  (if (vector? key)
    (reify IDeref
      (deref [_]
        (if-let [value (get-in @ants.app.static/app key)]
          value
          (throw (Exception. (str "Unresolved app component: " key))))))
    (reify IDeref
      (deref [_]
        (if-let [value (get @ants.app.static/app key)]
          value
          (throw (Exception. (str "Unresolved app component: " key))))))))

(def world (resolution :world))
