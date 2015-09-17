(ns ants.api.core-spec
  (:require [speclj.core :refer :all]
            [joodo.spec-helpers.controller :refer :all]
            [ants.api.core :refer :all]
            [taoensso.timbre :as log]))

(describe "ants api"

  (with-mock-rendering)
  (with-routes joodo-handler)
  (around [it] (log/with-log-level :report (it)))

  (it "handles home page"
    (let [response (do-get "/")]
      (should= 200 (:status response))
      (should-contain "Welcome to Ant Sparring!" (:body response))))
  )

(run-specs)
