(ns ants.api.core-spec
  (:use
    [speclj.core]
    [joodo.spec-helpers.controller]
    [ants.api.core]))

(describe "ants api"

  (with-mock-rendering)
  (with-routes joodo-handler)

  (it "handles home page"
    (let [response (do-get "/")]
      (should= 200 (:status response))
      (should= "Welcome to Ant Sparring!" (:body response))))
  )

(run-specs)
