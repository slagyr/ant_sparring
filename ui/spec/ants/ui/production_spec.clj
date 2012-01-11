(ns ants.ui.production-spec
  (:use
    [speclj.core]
    [limelight.clojure.core :as ll :exclude (scene stage production)]
    [limelight.clojure.specs.spec-helper])
  (:import
    [ants.ui.core LimelightAntsUI RealAntsInteractor RealAntsDataSource]))


(describe "Hosemonster Production"
  (with-limelight :production "arena" :scene "empty" :stage "arena")

  (it "interactor stored in the production's backstage"
    (let [interactor (backstage-get @production :interactor)]
      (should-not= nil interactor)
      (should= RealAntsInteractor (class interactor))
      (should= RealAntsDataSource (class (.ds interactor)))
      (should= LimelightAntsUI (class (.ui interactor)))))
  )
