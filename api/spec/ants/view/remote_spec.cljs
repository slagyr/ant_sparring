(ns ants.view.remote-spec
  (:require-macros [speclj.core :refer [describe it should= should-not= after before]])
  (:require [ants.view.remote :as remote]
            [speclj.core]))

(describe "Remote"

  (before (reset! remote/channel-state :uninitialized))

  (it "on-connect callback"
    (let [counter (atom 0)]
      (remote/on-connect #(swap! counter inc))
      (remote/event-msg-handler {:id :chsk/state :?data {:first-open? true}})
      (should= 1 @counter)))

  (it "on-connect callback only responds to first open"
    (let [counter (atom 0)]
      (remote/on-connect #(swap! counter inc))
      (remote/event-msg-handler {:id :chsk/state :?data {}})
      (should= 0 @counter)))

  (it "on-connect invokes immediately if already connected"
    (let [counter (atom 0)]
      (reset! remote/channel-state {:open? true})
      (remote/on-connect #(swap! counter inc))
      (should= 1 @counter)))

  )


