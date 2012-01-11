(ns ants.engine.core)

(deftype World [stuff commands])

(defn new-world []
  (World.
    (ref {:nest {:location [0 0]}})
    (ref {})))

; COMMAND GENERATION -----------------------------------------------------------------

(defn- gen-id []
  (int (rand 100000000)))

(defn- name-taken? [world name]
  (or
    (some #(= name (:name %)) (vals @(.stuff world)))
    (some #(= name (:name %)) (vals @(.commands world)))))

(defn- check-single-command [world id]
  (when (get @(.commands world) id)
    (throw (Exception. "You're allowed only 1 command per tick"))))

(defn join [world name]
  (dosync
    (when (name-taken? world name)
      (throw (Exception. (format "Join failed! The name '%s' is already taken" name))))
    (let [id (gen-id)]
      (alter (.commands world) assoc id {:command :join :name name :id id :timestamp (System/nanoTime)})
      id)))

(defn go [world ant direction]
  (dosync
    (check-single-command world ant)
    (alter (.commands world) assoc ant {:command :go :id ant :direction direction :timestamp (System/nanoTime)})))

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

; COMMAND EXECUTION -----------------------------------------------------------------

(defn- do-join [stuff command]
  (alter stuff assoc (:id command) {:type :ant :name (:name command) :location [0 0]})
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

(defn- award-points [ant points]
  (let [value (or (:points ant) 0)]
    (assoc ant :points (+ points value))))

(defn- award-food [ant found?]
  (if found?
    (assoc (award-points ant 1) :got-food true)
    ant))

(defn- do-go [stuff command]
  (let [ant-id (:id command)
        ant (get @stuff ant-id)
        dir (:direction command)
        current-loc (:location ant)
        new-loc (new-location current-loc dir)
        found-food? (and (not (:got-food? ant)) (food-at? @stuff new-loc))
        ant (award-food (assoc ant :location new-loc) found-food?)]
    (alter stuff assoc ant-id ant)
    (format "%s went %s%s" (:name ant) dir (if found-food? " and found some FOOD! (1 point)" ""))))

(defn- do-place-food [stuff command]
  (let [food-id (:id command)
        [x y] (:location command)]
    (alter stuff assoc food-id {:type :food :location [x y] :id food-id})
    (format "New food appeared at (%s, %s)" x y)))

(def command-map
  {
    :join do-join
    :go do-go
    :place-food do-place-food
    })

(defn execute-command [stuff command]
  (let [cmd-fn (get command-map (:command command))]
    (if cmd-fn
      (cmd-fn stuff command)
      (throw (Exception. (str "No such command: " (:command command)))))))

(defn tick [world]
  (dosync
    (let [sorted-commands (sort #(.compareTo (:timestamp %1) (:timestamp %2)) (vals @(.commands world)))
          results (map #(execute-command (.stuff world) %) sorted-commands)]
      (ref-set (.commands world) {})
      (doall results))))

