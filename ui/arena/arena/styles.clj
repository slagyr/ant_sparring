(style "arena"
  )

(style "controls"
  :width "100%"
  :height "8%"
  :vertical-alignment :center
  :horizontal-alignment :center
  :padding 5
  :bottom-border-width 1
  :border-color :grey
  )

(style "label"
  :text-color "maroon")

(style "host-input"
  :width 300)

(style "displays"
  :height "92%")

(style "world-buffer"
  :width "67%"
  :height "100%"
  :vertical-alignment :center
  :horizontal-alignment :center
  )

(style "world"
  :width 602
  :height 602
  :border-width 1
  :border-color :grey)

(style "info"
  :width "33%"
  :height "100%"
  :left-border-width 1
  :border-color :grey)

(style "scores"
  :width "100%"
  :height "50%"
;  :bottom-border-width 1
  :border-color :grey)

(style "score"
  :width "100%"
  :height "10%"
  :vertical-alignment :center
  :left-padding 10
  :right-padding 10
  :bottom-border-width 1
  :border-color :grey)

(style "score-name"
  :width "75%"
  :vertical-alignment :center
  :horizontal-alignment :left
  :font-size 20)

(style "score-value"
  :width "25%"
  :vertical-alignment :center
  :horizontal-alignment :right
  :font-size 30
  :font-style "bold")

(style "log"
  :width "100%"
  :height "50%"
  :vertical-scrollbar :on)

(style "log-entry"
  :width "100%"
  :horizontal-alignment :center
  :font-size 14
  )

(style "thing"
  :width 60
  :height 60
  :float :on
  :x 0
  :y 0)

(style "ant"
  (extends "thing")
  :background-color :blue
  :rounded-corner-radius 10
  :vertical-alignment :center
  :horizontal-alignment :center
  :font-style "bold"
  :font-size 14)

(style "nest"
  (extends "thing")
  :background-image "images/nest.png")

(style "food"
  (extends "thing")
  :background-image "images/food.png")
