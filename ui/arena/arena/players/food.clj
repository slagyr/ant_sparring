(on-mouse-clicked [e]
  (let [style (.getStyle (.getRecipient e))
        fx (Integer/parseInt (.getX style))
        fy (Integer/parseInt (.getY style))
        x (+ (.getX e) fx)
        y (+ (.getY e) fy)]
    (.remove-food (backstage-get (production e) "interactor") x y)))