(ns ants.api-spec
  (:require [speclj.core :refer :all]
            [joodo.spec-helpers.controller :refer :all]
            [ants.api :refer :all]
            [taoensso.timbre :as log]))

(describe "ants api"

  (with-mock-rendering)
  ;(with-routes joodo-handler)
  (around [it] (log/with-log-level :report (it)))
  )

(run-specs)
