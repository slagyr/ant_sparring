(ns ants.engine
  (:require [ants.log :as log])
  (:import
    [java.util.concurrent TimeUnit ScheduledThreadPoolExecutor]))

(def TICK-DURATION 1000)

(deftype World [stuff commands log scheduler observer])

(defn new-world
  ([] (new-world nil))
  ([observer]
   (World.
     (ref {})
     (ref {})
     (ref [])
     (atom nil)
     observer)))

(def DIRECTIONS #{"n" "ne" "e" "se" "s" "sw" "w" "nw"})

; COMMAND GENERATION -----------------------------------------------------------------

(defn- gen-id []
  (str (int (rand 100000000))))

(defn- name-taken? [world name]
  (or
    (some #(= name (:team %)) (vals @(.stuff world)))
    (some #(= name (:name %)) (vals @(.commands world)))))

(defn- check-single-command [world id]
  (when (get @(.commands world) id)
    (throw (Exception. "You're allowed only 1 command per tick"))))

(defn- check-valid-id [world id type]
  (let [thing (get @(.stuff world) id)]
    (when-not (and thing (= type (:type thing)))
      (throw (Exception. (format "Invalid ID (%s).  This %s doesn't exist." id (name type)))))))

(defn- check-cmd-id [world id type]
  (check-single-command world id)
  (check-valid-id world id type))

(defn- check-direction [direction]
  (when (not (DIRECTIONS direction))
    (throw (Exception. "You can't go that way silly ant!"))))

(defn join [world name]
  (dosync
    (when (name-taken? world name)
      (throw (Exception. (format "Join failed! The name '%s' is already taken" name))))
    (let [id (gen-id)]
      (alter (.commands world) assoc id {:command :join :name name :id id :timestamp (System/nanoTime)})
      id)))

(defn spawn [world nest]
  (dosync
    (check-cmd-id world nest :nest)
    (let [nest-val (get @(.stuff world) nest)
          id (gen-id)]
      (when (< (:food nest-val) 1)
        (throw (Exception. "You need food to spawn an ant.")))
      (alter (.commands world) assoc id {:command :spawn :nest nest :id id :timestamp (System/nanoTime)})
      id)))

(defn go [world ant direction]
  (dosync
    (check-cmd-id world ant :ant)
    (check-direction direction)
    (alter (.commands world) assoc ant {:command :go :id ant :direction direction :timestamp (System/nanoTime)})
    ant))

(defn look [world ant]
  (dosync
    (check-cmd-id world ant :ant)
    (alter (.commands world) assoc ant {:command :look :id ant :timestamp (System/nanoTime)})
    ant))

(defn place-food [world location]
  (dosync
    (let [id (gen-id)]
      (alter (.commands world) assoc id {:command :place-food :location location :id id :timestamp (System/nanoTime)})
      id)))

(defn remove-food [world location]
  (dosync
    (if-let [food (first (filter (fn [t] (and (= :food (:type t)) (= location (:location t)))) (vals @(.stuff world))))]
      (do
        (check-single-command world (:id food))
        (alter (.commands world) assoc (:id food) {:command :remove-food :location location :id (:id food) :timestamp (System/nanoTime)})
        (:id food))
      (throw (Exception. (str "No food found at " location))))))

(defn remove-all-food [world]
  (dosync
    (let [id (gen-id)]
      (alter (.commands world) assoc id {:command :remove-all-food :id id :timestamp (System/nanoTime)})
      id)))

(defn get-feed [world]
  (dosync
    (let [response {:stuff @(.stuff world) :log @(.log world)}]
      (ref-set (.log world) [])
      response)))


; COMMAND EXECUTION -----------------------------------------------------------------

(def nest-template
  {:type     :nest
   :location [0 0]
   :food     5
   :team     "Unknown"
   :id       "Unknown"})

(def ant-template
  {:type     :ant
   :location [0 0]
   :got-food false
   :team     "Unknown"
   :nest     -1
   :n        -1
   :id       "Unknown"})

(defn- do-join [stuff command]
  (alter stuff assoc (:id command) (assoc nest-template :team (:name command) :id (:id command)))
  (format "Team %s has entered the world!" (:name command)))

(defn- do-spawn [stuff {:keys [nest id]}]
  (let [nest-val (get @stuff nest)
        team-ants (filter #(and (= :ant (:type %)) (= (:team nest) (:team %))) (vals @stuff))]
    (alter stuff assoc
           id (assoc ant-template :team (:team nest-val)
                                  :id id
                                  :n (inc (count team-ants))
                                  :nest nest)
           nest (update-in nest-val [:food] dec))
    (format "Team %s has spawned a new ant." (:team nest-val))))

(defn- new-location [[x y] dir]
  (cond
    (= "n" dir) [x (dec y)]
    (= "ne" dir) [(inc x) (dec y)]
    (= "e" dir) [(inc x) y]
    (= "se" dir) [(inc x) (inc y)]
    (= "s" dir) [x (inc y)]
    (= "sw" dir) [(dec x) (inc y)]
    (= "w" dir) [(dec x) y]
    (= "nw" dir) [(dec x) (dec y)]))


(defn food? [thing] (= :food (:type thing)))
(defn nest? [thing] (= :nest (:type thing)))

(defn- food-at? [stuff loc]
  (some
    #(and (food? %) (= loc (:location %)))
    (vals stuff)))

(defn- nest-at? [stuff loc]
  (some
    #(and (nest? %) (= loc (:location %)))
    (vals stuff)))

(defn- award-points [ant points]
  (let [value (or (:points ant) 0)]
    (assoc ant :points (+ points value))))

(defn- update-ant-food [ant food? fed?]
  (cond
    food? (assoc (award-points ant 1) :got-food true)
    fed? (assoc (award-points ant 2) :got-food false)
    :else ant))

(defn update-nest-food [nest fed-nest?]
  (if fed-nest?
    (update-in nest [:food] inc)
    nest))

(defn- do-go [stuff command]
  (let [ant-id (:id command)
        ant (get @stuff ant-id)
        dir (:direction command)
        current-loc (:location ant)
        new-loc (new-location current-loc dir)
        found-food? (and (not (:got-food ant)) (food-at? @stuff new-loc))
        fed-nest? (and (:got-food ant) (nest-at? @stuff new-loc))
        ant (update-ant-food (assoc ant :location new-loc) found-food? fed-nest?)
        nest (update-nest-food (get @stuff (:nest ant)) fed-nest?)]
    (alter stuff assoc ant-id ant (:nest ant) nest)
    (format "%s-%d went %s%s" (:team ant) (:n ant) dir
            (cond
              found-food? " and found some FOOD!"
              fed-nest? " and FED HIS NEST!"
              :else ""))))

(defn- do-place-food [stuff command]
  (let [food-id (:id command)
        [x y] (:location command)]
    (alter stuff assoc food-id {:type :food :location [x y] :id food-id})
    (format "New food appeared at (%s, %s)" x y)))

(defn- do-remove-food [stuff command]
  (let [food-id (:id command)
        [x y] (:location command)]
    (alter stuff dissoc food-id)
    (format "The food at (%s, %s) has disappeared" x y)))

(defn- do-remove-all-food [stuff command]
  (let [foods (filter food? (vals @stuff))
        food-ids (map :id foods)]
    (alter stuff (fn [stuff] (reduce #(dissoc %1 %2) stuff food-ids)))
    (format "All food (%s) has been removed" (count foods))))

(defn- do-look [stuff command]
  (let [ant (get @stuff (:id command))]
    (format "%s-%d looks around" (:team ant) (:n ant))))

(def command-map
  {
   :join            do-join
   :go              do-go
   :place-food      do-place-food
   :remove-food     do-remove-food
   :remove-all-food do-remove-all-food
   :look            do-look
   :spawn           do-spawn
   })

(defn execute-command [stuff command]
  (try
    (let [cmd-fn (get command-map (:command command))]
      (if cmd-fn
        (cmd-fn stuff command)
        (throw (Exception. (str "No such command: " (:command command))))))
    (catch Exception e
      (log/error e)
      (throw e))))

(defn- exec-commands [world]
  (dosync
    (let [sorted-commands (sort #(.compareTo (:timestamp %1) (:timestamp %2)) (vals @(.commands world)))
          results (doall (map #(execute-command (.stuff world) %) sorted-commands))]
      (alter (.log world) concat results)
      (ref-set (.commands world) {})
      results)))

(defn tick [world]
  (try
    (let [results (exec-commands world)]
      (locking world (.notifyAll world))
      (when-let [observer (.observer world)]
        (observer @(.stuff world) results)))
    (catch Exception e (log/error e))))

(defn start [app]
  (log/info "Starting Ant Engine")
  (if-let [engine (:engine app)]
    (log/warn "Ant Engine is already started!")
    (let [world (:world app)]
      (assoc app :engine
                 (doto (ScheduledThreadPoolExecutor. 1)
                   (.scheduleWithFixedDelay #(tick world) 0 TICK-DURATION TimeUnit/MILLISECONDS))))))

(defn stop [app]
  (log/info "Stopping Ant Engine")
  (if-let [engine (:engine app)]
    (.shutdown engine)
    (log/warn "Ant Engine is missing.  Can't stop."))
  (dissoc app :engine))

(defn inspect [world]
  (println "World")
  (doseq [[k v] @(.stuff world)]
    (prn k v))
  (doseq [l @(.log world)]
    (println l))
  (doseq [c @(.commands world)]
    (prn c)))

(defn describe-surroundings [world ant]
  (let [loc (:location ant)]
    (assoc ant :surroundings
               (reduce
                 (fn [r d]
                   (let [new-loc (new-location loc d)]
                     (assoc r d (vec (filter #(= new-loc (:location %)) (vals @(.stuff world)))))))
                 {}
                 DIRECTIONS))))

(defn stat [world id]
  (let [thing (get @(.stuff world) id)]
    (when (nil? thing) (throw (Exception. (str "Missing id for stat: " id))))
    (if (= :ant (:type thing))
      (describe-surroundings world thing)
      thing)))