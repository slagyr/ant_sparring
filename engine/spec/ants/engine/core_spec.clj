(ns ants.engine.core-spec
  (:use
    [speclj.core]
    [ants.engine.core]))

(describe "Ants Engine Core"

  (with world (new-world))
  (with stuff @(.stuff @world))
  (with commands @(.commands @world))

  (it "can create a new world"
    (should= {:nest {:location [0, 0]}} @stuff)
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
      (should= ["Phil has entered the world!"] results)
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
      (let [results (tick @world)]
        (should= ["George went north"] results))
      (should= [0 -1] (:location (get @stuff @ant-id))))

    (it "ant can go south"
      (go @world @ant-id "south")
      (let [results (tick @world)]
        (should= ["George went south"] results))
      (should= [0 1] (:location (get @stuff @ant-id))))

    (it "ant can go east"
      (go @world @ant-id "east")
      (let [results (tick @world)]
        (should= ["George went east"] results))
      (should= [1 0] (:location (get @stuff @ant-id))))

    (it "ant can go west"
      (go @world @ant-id "west")
      (let [results (tick @world)]
        (should= ["George went west"] results))
      (should= [-1 0] (:location (get @stuff @ant-id))))

    (it "can't add command for existing id"
      (go @world @ant-id "west")
      (should-throw java.lang.Exception "You're allowed only 1 command per tick"
        (go @world @ant-id "north"))
      (should= 1 (count @commands)))

    (context "finding food"
      (it "gets stored and award points"
        (place-food @world [1 0])
        (tick @world)
        (go @world @ant-id "east")
        (let [results (tick @world)
              ant (get @stuff @ant-id)]
          (should= true (:got-food ant))
          (should= 1 (:points ant))
          (should= ["George went east and found some FOOD! (1 point)"] results)))

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
          (should= ["George went north"] results)))
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
            results (tick @world)
            food (get @stuff food-id)]
        (should= :food (:type food))
        (should= [5 5] (:location food))
        (should= food-id (:id food))
        (should= ["New food appeared at (5, 5)"] results)))

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
    )
  )

(run-specs)