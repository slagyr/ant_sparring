(ns ants.engine.core)

(deftype World [stuff commands])

(defn new-world []
  (World.
    (ref {:nest {:location [0 0]}})
    (ref {})))

(defn- gen-id []
  (int (rand 100000000)))

(defn- name-taken? [world name]
  (or
    (some #(= name (:name %)) (vals @(.stuff world)))
    (some #(= name (:name %)) (vals @(.commands world)))))

(defn join [world name]
  (dosync
    (when (name-taken? world name)
      (throw (Exception. (format "Join failed! The name '%s' is already taken" name))))
    (let [id (gen-id)]
      (alter (.commands world) assoc id {:command :join :name name :id id :timestamp (System/nanoTime)})
      id)))

(defn go [world ant direction]
  (dosync
    (alter (.commands world) assoc ant {:command :go :id ant :direction direction :timestamp (System/nanoTime)})))

(defn- do-join [stuff command]
  (alter stuff assoc (:id command) {:name (:name command) :location [0 0]})
  (format "%s has entered the world!" (:name command)))

(defn- new-location [[x y] dir]
  (cond
    (= "north" dir) [x (- y 1)]
    (= "south" dir) [x (+ y 1)]
    (= "east" dir) [(+ x 1) y]
    (= "west" dir) [(- x 1) y]))

(defn- do-go [stuff command]
  (let [ant-id (:id command)
        ant (get @stuff ant-id)
        current-loc (:location ant)
        dir (:direction command)
        new-loc (new-location current-loc dir)]
    (alter stuff assoc ant-id (assoc ant :location new-loc))
    (format "%s went %s" (:name ant) dir)))

(def command-map
  {
    :join do-join
    :go do-go
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

