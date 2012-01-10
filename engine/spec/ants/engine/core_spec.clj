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
        (should= "Phil" (:name ant))
        (should= [0 0] (:location ant)))))

  (context "with one ant"

    (with ant (join @world "George"))
    (before (do @ant (tick @world)))

    (it "can give go command"
      (go @world @ant "north")
      (let [command (get @commands @ant)]
        (should-not= nil command)
        (should= "north" (:direction command))
        (should= @ant (:id command))))

    (it "ant can go north"
      (go @world @ant "north")
      (let [results (tick @world)]
        (should= ["George went north"] results))
      (should= [0 -1] (:location (get @stuff @ant))))

    (it "ant can go south"
      (go @world @ant "south")
      (let [results (tick @world)]
        (should= ["George went south"] results))
      (should= [0 1] (:location (get @stuff @ant))))

    (it "ant can go east"
      (go @world @ant "east")
      (let [results (tick @world)]
        (should= ["George went east"] results))
      (should= [1 0] (:location (get @stuff @ant))))

    (it "ant can go west"
      (go @world @ant "west")
      (let [results (tick @world)]
        (should= ["George went west"] results))
      (should= [-1 0] (:location (get @stuff @ant))))

    )

  )

(run-specs)