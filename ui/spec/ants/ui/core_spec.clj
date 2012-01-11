(ns ants.ui.core-spec
  (:use
    [speclj.core]
    [ants.ui.core]
    [ants.ui.mocking]
    [limelight.clojure.core :as ll :exclude (scene stage production)]
    [limelight.clojure.specs.spec-helper])
  (:import
    [limelight.util Mouse]))

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

  (context "Limelight UI"
    (with-limelight :production "arena" :scene "arena" :stage "arena")

    (context "with interactor"
      (with interactor (new-mock-interactor))
      (before (backstage-put @production "interactor" @interactor))

      (it "start-button starts the interactor"
        (text= (find-by-id @scene "host") "http://here")
        (Mouse/click (find-by-id @scene "start-button"))
        (should= "startup" @(.call @interactor))
        (should= "http://here" (:host @(.params @interactor))))

      (it "stop-button stops the interactor"
        (Mouse/click (find-by-id @scene "stop-button"))
        (should= "shutdown" @(.call @interactor)))
      )

    (context "with ui"
      (with ui (new-ants-ui @production))
      (before @scene)

      (it "populates nest"
        (update-ui @ui {:stuff [{:type :nest :location [0, 0]}]})
        (let [nests (find-by-name @scene "nest")
              nest (first nests)]
          (should= 1 (count nests))
          (let [style (.getStyle (.getPeer nest))]
            (should= "300" (.getX style))
            (should= "300" (.getY style)))))

      (it "populates food"
        (update-ui @ui {:stuff [{:type :food :location [-1, -1]}]})
        (let [foods (find-by-name @scene "food")
              food (first foods)]
          (should= 1 (count foods))
          (let [style (.getStyle (.getPeer food))]
            (should= "240" (.getX style))
            (should= "240" (.getY style)))))

      (it "populates ant"
        (update-ui @ui {:stuff [{:type :ant :location [1, 1] :name "George" :color "black"}]})
        (let [ants (find-by-name @scene "ant")
              ant (first ants)]
          (should= 1 (count ants))
          (let [style (.getStyle (.getPeer ant))]
            (should= "360" (.getX style))
            (should= "360" (.getY style))
            (should= "#000000ff" (.getBackgroundColor style)))))

      (it "populates scores"
        (update-ui @ui {:scores [{:name "joe" :points 9 :color "red"}{:name "bill" :points 8 :color "green"}]})
        (let [scores (children (find-by-id @scene "scores"))]
          (should= 2 (count scores))
          (should= "#ff0000ff" (.getBackgroundColor (.getStyle (.getPeer (first scores)))))
          (should= "joe" (text (first (find-by-name (first scores) "score-name"))))
          (should= "9" (text (first (find-by-name (first scores) "score-value"))))
          (should= "#00ff00ff" (.getBackgroundColor (.getStyle (.getPeer (second scores)))))
          (should= "bill" (text (first (find-by-name (second scores) "score-name"))))
          (should= "8" (text (first (find-by-name (second scores) "score-value"))))))

      (it "populates log"
        (update-ui @ui {:log ["message 1" "message 2"]})
        (let [entries (children (find-by-id @scene "log"))]
          (should= 2 (count entries))
          (should= "message 1" (text (first entries)))
          (should= "message 2" (text (second entries)))))

      )
    )
  )

