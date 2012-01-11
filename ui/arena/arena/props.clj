[:controls
 [:label {:text "Engine Host:"}]
 [:host-input {:id "host" :players "text-box"}]
 [:start-button {:id "start-button" :players "button" :text "Start" :on-mouse-clicked (fn [e] (.startup (backstage-get (production e) "interactor") (text (find-by-id (scene e) "host"))))}]
 [:stop-button {:id "stop-button" :players "button" :text "Stop" :on-mouse-clicked (fn [e] (.shutdown (backstage-get (production e) "interactor")))}]]
[:displays
 [:world-buffer
  [:world {:id "world" :on-mouse-clicked (fn [e] (.place-food (backstage-get (production e) "interactor") (.getX e) (.getY e)))}
;    [:ant {:x 120 :y 120 :text "Micah"}]
;    [:nest {:x 270 :y 270}]
;    [:food {:x 400 :y 400}]
    ]]
 [:info
  [:scores {:id "scores"}
;   (for [i (range 10)]
;     [:score
;      [:score-name {:text (str "Name-" i)}]
;      [:score-value {:text (str (* 100 i))}]])
   ]
  [:log {:id "log"}
;   (for [i (range 50)]
;     [:log-entry {:text (str "This is log entry #" i)}])
   ]]]