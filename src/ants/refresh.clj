(ns ants.refresh
  (:require [ants.log :as log]
            [ants.util :as util]
            [clojure.string :as string]
            [fresh.core :refer [freshener ns-to-file]]
            [joodo.env :as env]))

; START HACK --------------------
(ns ants.refresh.static)

(when-not (resolve '*freshener*)
  (def ^:dynamic *freshener*))

(in-ns 'ants.refresh)
(require '[ants.refresh.static :refer [*freshener*]])
; END HACK ----------------------

(defn- files-to-keep-fresh []
  (->> (all-ns)
       (map #(ns-to-file (.name %)))
       (filter identity)))

(defn make-auditor [before]
  (fn [report]
    (if-let [reloaded (seq (:reloaded report))]
      (do
        (when-let [f (util/resolve-var-or-nil before)] (f))
        (log/info (str "Reloading...\n\t" (string/join "\n\t" (map #(.getAbsolutePath %) reloaded)))))
      (log/info "Nothing changed."))
    true))

(defn init [before after]
  (when (and (env/development?) (not (bound? #'*freshener*)))
    (alter-var-root #'*freshener*
                    (fn [_]
                      (let [auditor (make-auditor before)
                            refresh (freshener files-to-keep-fresh auditor)]
                        (fn []
                          (let [report (refresh)]
                            (when (seq (:reloaded report))
                              (when-let [f (util/resolve-var-or-nil after)] (f))))))))))

(defn refresh! []
  (when (bound? #'*freshener*)
    (*freshener*)))

(defn wrap-refresh [handler]
  (fn [request]
    (refresh!)
    (handler request)))

(defn wrap-development [handler]
  (util/resolve-var 'chee.pretty-map/pretty-map)
  (let [wrap-verbose (util/resolve-var 'joodo.middleware.verbose/wrap-verbose)]
    (require 'joodo.middleware.verbose)
    (-> handler
        wrap-verbose
        (wrap-refresh))))