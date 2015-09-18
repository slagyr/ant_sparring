(ns ants.engine.engine-spec
  (:require [speclj.core :refer :all]
            [ants.engine :refer :all]))

(describe "Ants Engine Core"

  (with world (new-world))
  (with stuff @(.stuff @world))
  (with commands @(.commands @world))
  (with log @(.log @world))

  (it "can create a new world"
    (should= {:nest {:type :nest :id :nest :location [0, 0]}} @stuff)
    (should= {} @commands))

  (it "can join the world"
    (let [result (join @world "Joe")
          command (get @commands result)]
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

  (it "ant enters world on tick"
    (let [id (join @world "Phil")
          results (tick @world)]
      (should= {} @commands)
      (should= 2 (count @stuff))
      (should= "Phil has entered the world!" (last @log))
      (let [ant (get @stuff id)]
        (should= :ant (:type ant))
        (should= "Phil" (:name ant))
        (should= [0 0] (:location ant)))))


  (context "with one ant"

    (with ant-id (join @world "George"))
    (before (do @ant-id (tick @world)))

    (it "can give go command"
      (go @world @ant-id "north")
      (let [command (get @commands @ant-id)]
        (should-not= nil command)
        (should= :go (:command command))
        (should= "north" (:direction command))
        (should= @ant-id (:id command))))

    (it "ant can go north"
      (go @world @ant-id "north")
      (tick @world)
      (should= "George went north" (last @log))
      (should= [0 -1] (:location (get @stuff @ant-id))))

    (it "ant can go south"
      (go @world @ant-id "south")
      (tick @world)
      (should= "George went south" (last @log))
      (should= [0 1] (:location (get @stuff @ant-id))))

    (it "ant can go east"
      (go @world @ant-id "east")
      (tick @world)
      (should= "George went east" (last @log))
      (should= [1 0] (:location (get @stuff @ant-id))))

    (it "ant can go west"
      (go @world @ant-id "west")
      (tick @world)
      (should= "George went west" (last @log))
      (should= [-1 0] (:location (get @stuff @ant-id))))

    (it "ant can't go in blah direction"
      (should-throw java.lang.Exception "You can't go that way silly ant!"
                    (go @world @ant-id "blah"))
      (should= 0 (count @commands)))

    (it "can't add command for existing id"
      (go @world @ant-id "west")
      (should-throw java.lang.Exception "You're allowed only 1 command per tick"
                    (go @world @ant-id "north"))
      (should= 1 (count @commands)))

    (it "can't add command missing ant"
      (should-throw java.lang.Exception "Invalid ID (missing_id).  It appears you don't exist."
                    (go @world "missing_id" "north"))
      (should= 0 (count @commands)))

    (it "ant can look around"
      (look @world @ant-id)
      (let [command (get @commands @ant-id)]
        (should-not= nil command)
        (should= :look (:command command))
        (should= @ant-id (:id command)))
      (tick @world)
      (should= "George looks around" (last @log)))

    (context "finding food"
      (it "gets stored and award points"
        (place-food @world [1 0])
        (tick @world)
        (go @world @ant-id "east")
        (let [results (tick @world)
              ant (get @stuff @ant-id)]
          (should= true (:got-food ant))
          (should= 1 (:points ant))
          (should= "George went east and found some FOOD! (1 point)" (last @log))))

      (it "does nothing if it's already got food"
        (place-food @world [1 0])
        (place-food @world [1 1])
        (tick @world)
        (go @world @ant-id "east")
        (tick @world)
        (go @world @ant-id "north")
        (let [results (tick @world)
              ant (get @stuff @ant-id)]
          (should= true (:got-food ant))
          (should= 1 (:points ant))
          (should= "George went north" (last @log))))

      (it "points for taking food home"
        (place-food @world [1 0])
        (tick @world)
        (go @world @ant-id "east")
        (tick @world)
        (go @world @ant-id "west")
        (tick @world)
        (let [ant (get @stuff @ant-id)]
          (should= false (:got-food ant))
          (should= 3 (:points ant))
          (should= "George went west and FED HIS NEST! (2 points)" (last @log))))

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
          (should= "George" (:name result))
          (should= [0 0] (:location result))
          (should= false (:got-food result))
          (should= 0 (:points result))))

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
    )
  )

(run-specs)