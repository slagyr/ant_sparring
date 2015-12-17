(ns ants.engine-spec
  (:require [speclj.core :refer :all]
            [ants.engine :refer :all]
            [ants.engine :as engine]))

(describe "Ants Engine Core"

  (with world (new-world))
  (with stuff @(.stuff @world))
  (with commands @(.commands @world))
  (with log @(.log @world))

  (it "can create a new world"
    (should= {} @stuff)
    (should= {} @commands))

  (it "can join the world"
    (let [result (join @world "Joe")                        ;"client")
          command (get @commands result)]
      (should-not= nil result)
      (should= 1 (count @commands))
      (should= :join (:command command))
      (should= "Joe" (:name command))
      ;(should= "client" (:client command))
      (should= result (:id command))
      (should-not= nil (:timestamp command))))

  #_(it "cant join twice from same client"
      (join @world "Joe" "10.10.0.1")
      (join @world "Mike" "10.10.0.1")
      (tick

        (should-not= nil result)
        (should= 1 (count @commands))
        (should= :join (:command command))
        (should= "Joe" (:name command))
        (should= result (:id command))
        (should-not= nil (:timestamp command))))

  (it "can't join game if name is in commands"
    (join @world "Joe")
    (should-throw java.lang.Exception "Join failed! The name 'Joe' is already taken"
                  (join @world "Joe"))
    (should= 1 (count @commands)))

  (it "nest enters world on tick"
    (let [id (join @world "Phil")
          results (tick @world)]
      (should= {} @commands)
      (should= 1 (count @stuff))
      (should= "Team Phil has entered the world!" (last @log))
      (let [nest (get @stuff id)]
        (should= :nest (:type nest))
        (should= "Phil" (:team nest))
        (should= 5 (:food nest))
        (should= [0 0] (:location nest)))))

  (it "spawning a new ant"
    (let [nest-id (join @world "Phil")
          _ (tick @world)
          ant-id (spawn @world nest-id)]
      (tick @world)
      (should= {} @commands)
      (should= 2 (count @stuff))
      (should= "Team Phil has spawned a new ant." (last @log))
      (let [nest (get @stuff nest-id)
            ant (get @stuff ant-id)]
        (should= 4 (:food nest))
        (should= :ant (:type ant))
        (should= "Phil" (:team ant))
        (should= nest-id (:nest ant))
        (should= [0 0] (:location ant)))))

  (it "spawns 2 ants"
    (let [nest-id (join @world "Phil")]
      (tick @world)
      (spawn @world nest-id)
      (tick @world)
      (spawn @world nest-id)
      (tick @world)
      (let [nest (get @stuff nest-id)
            ants (filter ant? (vals @stuff))]
        (should= 2 (count ants))
        (should= #{1 2} (set (map :n ants)))
        (should= 2 (:ants nest)))))

  (it "can't spawn multiple ants in same tick"
    (let [nest-id (join @world "Phil")]
      (tick @world)
      (spawn @world nest-id)
      (should-throw Exception "You're allowed only 1 command per tick"
                    (spawn @world nest-id))))

  (it "spawning ant without food"
    (let [nest-id (join @world "Phil")]
      (tick @world)
      (doseq [i (range 5)] (spawn @world nest-id) (tick @world))
      (should-throw Exception "You need food to spawn an ant."
                    (spawn @world nest-id))))


  (context "with one ant"

    (with nest-id (join @world "George"))
    (with ant-id (do @nest-id (tick @world) (spawn @world @nest-id)))
    (before (do @ant-id (tick @world)))

    (it "can give go command"
      (go @world @ant-id "n")
      (let [command (get @commands @ant-id)]
        (should-not= nil command)
        (should= :go (:command command))
        (should= "n" (:direction command))
        (should= @ant-id (:id command))))

    (it "ant can go north"
      (go @world @ant-id "n")
      (tick @world)
      (should= "George-1 went n" (last @log))
      (should= [0 -1] (:location (get @stuff @ant-id))))

    (it "ant can go south"
      (go @world @ant-id "s")
      (tick @world)
      (should= "George-1 went s" (last @log))
      (should= [0 1] (:location (get @stuff @ant-id))))

    (it "ant can go east"
      (go @world @ant-id "e")
      (tick @world)
      (should= "George-1 went e" (last @log))
      (should= [1 0] (:location (get @stuff @ant-id))))

    (it "ant can go west"
      (go @world @ant-id "w")
      (tick @world)
      (should= "George-1 went w" (last @log))
      (should= [-1 0] (:location (get @stuff @ant-id))))

    (it "ant can't go in blah direction"
      (should-throw java.lang.Exception "You can't go that way silly ant!"
                    (go @world @ant-id "blah"))
      (should= 0 (count @commands)))

    (it "can't add command for existing id"
      (go @world @ant-id "w")
      (should-throw java.lang.Exception "You're allowed only 1 command per tick"
                    (go @world @ant-id "north"))
      (should= 1 (count @commands)))

    (it "can't add command missing ant"
      (should-throw java.lang.Exception "Invalid ID (missing_id).  This ant doesn't exist."
                    (go @world "missing_id" "north"))
      (should= 0 (count @commands)))

    (it "ant can look around"
      (look @world @ant-id)
      (let [command (get @commands @ant-id)]
        (should-not= nil command)
        (should= :look (:command command))
        (should= @ant-id (:id command)))
      (tick @world)
      (should= "George-1 looks around" (last @log)))

    (context "finding food"
      (it "gets stored and award points"
        (place-food @world [1 0])
        (tick @world)
        (go @world @ant-id "e")
        (let [results (tick @world)
              ant (get @stuff @ant-id)]
          (should= true (:got-food ant))
          (should= "George-1 went e and found some FOOD!" (last @log))))

      (it "does nothing if it's already got food"
        (place-food @world [1 0])
        (place-food @world [1 1])
        (tick @world)
        (go @world @ant-id "e")
        (tick @world)
        (go @world @ant-id "n")
        (let [results (tick @world)
              ant (get @stuff @ant-id)]
          (should= true (:got-food ant))
          (should= "George-1 went n" (last @log))))

      (it "points for taking food home"
        (place-food @world [1 0])
        (tick @world)
        (go @world @ant-id "e")
        (tick @world)
        (go @world @ant-id "w")
        (tick @world)
        (let [ant (get @stuff @ant-id)
              nest (get @stuff @nest-id)]
          (should= false (:got-food ant))
          (should= 5 (:food nest))
          (should= "George-1 went w and FED HIS NEST!" (last @log))))

      )

    (context "admin feed"

      (it "is provided"
        (let [old-log @(.log @world)
              result (get-feed @world)]
          (should= @stuff (:stuff result))
          (should= old-log (:log result))
          (should= [] @(.log @world))))
      )

    (context "stat"

      (it "can be retrieved"
        (let [result (stat @world @ant-id)]
          (should= :ant (:type result))
          (should= @ant-id (:id result))
          (should= "George" (:team result))
          (should= [0 0] (:location result))
          (should= false (:got-food result))
          (should= @nest-id (:nest result))))
      )
    )

  (context "food"

    (it "can be placed"
      (let [id (place-food @world [5 5])
            command (get @commands id)]
        (should-not= nil command)
        (should= :place-food (:command command))
        (should= [5 5] (:location command))
        (should= id (:id command))
        (should-not= nil (:timestamp command))))

    (it "will appear"
      (let [food-id (place-food @world [5 5])
            _ (tick @world)
            food (get @stuff food-id)]
        (should= :food (:type food))
        (should= [5 5] (:location food))
        (should= food-id (:id food))
        (should= "New food appeared at (5, 5)" (last @log))))

    (it "can be removed"
      (let [id (place-food @world [5 5])
            _ (tick @world)
            remove-id (remove-food @world [5 5])
            command (get @commands remove-id)]
        (should-not= nil command)
        (should= id remove-id)
        (should= :remove-food (:command command))
        (should= [5 5] (:location command))
        (should= id (:id command))
        (should-not= nil (:timestamp command))))

    (it "will disappear"
      (let [food-id (place-food @world [5 5])
            _ (tick @world)
            remove-id (remove-food @world [5 5])
            _ (tick @world)
            food (get @stuff food-id)]
        (should= nil food)
        (should= "The food at (5, 5) has disappeared" (last @log))))

    (it "can be removed"
      (let [id (place-food @world [5 5])
            _ (tick @world)
            remove-id (remove-all-food @world)
            command (get @commands remove-id)]
        (should-not= nil command)
        (should= :remove-all-food (:command command))
        (should-not= nil (:timestamp command))
        (tick @world)
        (tick @world)
        (should= [] (filter #(= :food (:type %)) (vals @stuff)))))
    )

  (context "observer"

    (it "world notifies abserver"
      (let [state (atom nil)
            observer (fn [stuff log] (reset! state [stuff log]))
            world (new-world observer)]
        (tick world)
        (Thread/sleep 100)
        (should= [{} []] @state)))
    )
  )

(run-specs)