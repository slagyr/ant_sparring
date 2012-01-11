(ns ants.ui.core-spec
  (:use
    [speclj.core]
    [ants.ui.core]
    [ants.ui.mocking]))

(describe "Ants UI"

  ;  (it "can curl a URL"
  ;    (let [result (curl "http://localhost:8080")]
  ;      (should= true (.startsWith result "Welcome to Ant Sparring!"))))

  (context "interactor"
    (with ui (new-mock-ui))
    (with ds (new-mock-data-source))
    (with interactor (new-ants-interactor @ui @ds))
    (after (shutdown @interactor))

    (it "can create a real interactor"
      (should= ants.ui.core.RealAntsInteractor (class @interactor))
      (should= @ui (.ui @interactor))
      (should= @ds (.ds @interactor)))

    (it "will start tracking a site"
      (startup @interactor "http://somewhere")
      (should= "http://somewhere/_admin_/start" (first @(.urls @ds)))
      (should= "http://somewhere" @(.host @interactor))
      (should-not= nil @(.scheduler @interactor)))

    (it "will stop tracking a site"
      (startup @interactor "http://somewhere")
      (shutdown @interactor)
      (should= "http://somewhere/_admin_/stop" (last @(.urls @ds)))
      (should= nil @(.scheduler @interactor)))

    (context "update"

      (before (reset! (.host @interactor) "http://host"))

      (it "with just the nest"
        (reset! (.stub @ds) {:stuff {:nest {:id :nest :type :nest :location [0 0]}}})
        (update @interactor)
        (should= "http://host/_admin_/feed" (last @(.urls @ds)))
        (let [ui-data @(.update @ui)]
          (should= [{:id :nest :type :nest :location [0 0]}] (:stuff ui-data))))

      (it "consistent color is assigned to ants"
        (reset! (.stub @ds) {:stuff
                             {:nest {:id :nest :type :nest :location [0 0]}
                              "123" {:id "123" :type :ant :location [0 0]}}})
        (update @interactor)
        (let [ui-data @(.update @ui)
              ant (first (:stuff ui-data))
              color (:color ant)]
          (should= "123" (:id ant))
          (should= :ant (:type ant))
          (should-not= nil color)
          (should= color (get @(.colors @interactor) "123"))
          (update @interactor)
          (should= color (:color (first (:stuff ui-data))))))

      (it "provides the top 10 scores"
        (let [ants (map (fn [n] {:id (str n) :points n :type :ant :location [n 0]}) (range 100))
              stuff (reduce (fn [s a] (assoc s (:id a) a)) {:nest {:id :nest :type :nest :location [0 0]}} ants)
              _ (reset! (.stub @ds) {:stuff stuff})
              _ (update @interactor)
              ui-data @(.update @ui)
              scores (:scores ui-data)]
          (should-not= nil scores)
          (should= 10 (count scores))
          (should= "99" (:id (first scores)))
          (should= "98" (:id (second scores)))
          (should= "97" (:id (nth scores 2)))))

      (it "provides the top 50 log entries"
        (let [log-source (map #(str "This is log entry #" %) (range 30))
              _ (reset! (.stub @ds) {:stuff {} :log log-source})]
          (update @interactor)
          (should= 30 (count (:log @(.update @ui))))
          (update @interactor)
          (should= 50 (count (:log @(.update @ui))))))

      )
    )
  )