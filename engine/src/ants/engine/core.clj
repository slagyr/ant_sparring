(ns ants.engine.core
  (:import
    [java.util.concurrent TimeUnit ScheduledThreadPoolExecutor]))

(def TICK-DURATION 1000)

(deftype World [stuff commands log scheduler])

(defn new-world []
  (World.
    (ref {:nest {:type :nest :id :nest :location [0 0]}})
    (ref {})
    (ref [])
    (atom nil)))

(def *world* (new-world))

; COMMAND GENERATION -----------------------------------------------------------------

(defn- gen-id []
  (str (int (rand 100000000))))

(defn- name-taken? [world name]
  (or
    (some #(= name (:name %)) (vals @(.stuff world)))
    (some #(= name (:name %)) (vals @(.commands world)))))

(defn- check-single-command [world id]
  (when (get @(.commands world) id)
    (throw (Exception. "You're allowed only 1 command per tick"))))

(defn- check-valid-id [world id]
  (when (not (get @(.stuff world) id))
    (throw (Exception. (format "Invalid ID (%s).  It appears you don't exist." id)))))

(defn- check-cmd-id [world id]
  (check-single-command world id)
  (check-valid-id world id))

(def DIRECTIONS #{"north" "east" "west" "south"})
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

(defn go [world ant direction]
  (dosync
    (check-cmd-id world ant)
    (check-direction direction)
    (alter (.commands world) assoc ant {:command :go :id ant :direction direction :timestamp (System/nanoTime)})
    ant))

(defn look [world ant]
  (dosync
    (check-cmd-id world ant)
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

(defn get-feed [world]
  (dosync
    (let [response {:stuff @(.stuff world) :log @(.log world)}]
      (ref-set (.log world) [])
      response)))

(defn stat [world ant]
  (check-valid-id world ant)
  (get @(.stuff world) ant))

; COMMAND EXECUTION -----------------------------------------------------------------

(def ant-template
  {:type :ant
   :location [0 0]
   :points 0
   :got-food false
   :name "Unknown"
   :id "Unknown"})

(defn- do-join [stuff command]
  (alter stuff assoc (:id command) (assoc ant-template :name (:name command) :id (:id command)))
  (format "%s has entered the world!" (:name command)))

(defn- new-location [[x y] dir]
  (cond
    (= "north" dir) [x (- y 1)]
    (= "south" dir) [x (+ y 1)]
    (= "east" dir) [(+ x 1) y]
    (= "west" dir) [(- x 1) y]))

(defn- food-at? [stuff loc]
  (some
    #(and (= :food (:type %)) (= loc (:location %)))
    (vals stuff)))

(defn- nest-at? [stuff loc]
  (some
    #(and (= :nest (:type %)) (= loc (:location %)))
    (vals stuff)))

(defn- award-points [ant points]
  (let [value (or (:points ant) 0)]
    (assoc ant :points (+ points value))))

(defn- award-food [ant food? fed?]
  (cond
    food? (assoc (award-points ant 1) :got-food true)
    fed? (assoc (award-points ant 2) :got-food false)
    :else ant))

(defn- do-go [stuff command]
  (let [ant-id (:id command)
        ant (get @stuff ant-id)
        dir (:direction command)
        current-loc (:location ant)
        new-loc (new-location current-loc dir)
        found-food? (and (not (:got-food ant)) (food-at? @stuff new-loc))
        fed-nest? (and (:got-food ant) (nest-at? @stuff new-loc))
        ant (award-food (assoc ant :location new-loc) found-food? fed-nest?)]
    (alter stuff assoc ant-id ant)
    (format "%s went %s%s" (:name ant) dir
      (cond
        found-food? " and found some FOOD! (1 point)"
        fed-nest? " and FED HIS NEST! (2 points)"
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

(defn- do-look [stuff command]
  (let [ant (get @stuff (:id command))]
    (format "%s looks around" (:name ant))))

(def command-map
  {
    :join do-join
    :go do-go
    :place-food do-place-food
    :remove-food do-remove-food
    :look do-look
    })

(defn execute-command [stuff command]
  (let [cmd-fn (get command-map (:command command))]
    (if cmd-fn
      (cmd-fn stuff command)
      (throw (Exception. (str "No such command: " (:command command)))))))

(defn tick [world]
  (dosync
    (let [sorted-commands (sort #(.compareTo (:timestamp %1) (:timestamp %2)) (vals @(.commands world)))
          results (doall (map #(execute-command (.stuff world) %) sorted-commands))]
      (alter (.log world) concat results)
      (ref-set (.commands world) {})
      (locking world (.notifyAll world)))))

(defn start [world]
  (when (nil? @(.scheduler world))
    (reset! (.scheduler world) (ScheduledThreadPoolExecutor. 1))
    (.scheduleWithFixedDelay @(.scheduler world) #(tick world) 0 TICK-DURATION TimeUnit/MILLISECONDS)))

(defn stop [world]
  (when @(.scheduler world)
    (.shutdown @(.scheduler world))
    (reset! (.scheduler world) nil)))

