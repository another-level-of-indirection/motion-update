(ns motion.demo-kaleidoscope
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string]
            [motion.utils :refer [timeline pol2crt]]
            [motion.styles :refer [colors]]
            [motion.components :as components]))

(def m js/Math)
(def tau (* m.PI 2))

(defn handle-kaleidoscope-click [state _ev]
  (swap! state update :symmetry #(if (>= % 12) 3 (+ % 1))))

(defn component-kaleidoscope-segment [t segment-angle]
  (let [wobble-x (* 15 (m.sin (* t 0.0005)))
        wobble-y (* 10 (m.cos (* t 0.00065)))
        pulse1 (* 50 (+ 1 (* 0.15 (m.sin (* t 0.0008)))))
        pulse2 (* 80 (+ 1 (* 0.1 (m.cos (* t 0.0009)))))
        rotation (* t 0.015)]
    [:g {:transform (str "rotate(" (* segment-angle (/ 180 m.PI)) ")")}
     ;; Main petal shape
     [:path {:d (js/roundPathCorners
                 (str "M 0 0 L " (+ 40 wobble-x) " " (- 0 60 wobble-y)
                      " L " (+ 60 wobble-x) " " (- 0 70 wobble-y)
                      " L 30 -10 Z") 5 false)
             :fill (colors :blue)
             :fill-opacity 0.6
             :stroke-width "2px"}]

     ;; Decorative circles
     [:circle {:cx (+ 50 wobble-x) :cy (- 0 65 wobble-y) :r 8
               :fill (colors :red)
               :fill-opacity 0.8}]

     ;; Pulsing arc
     ;;  [components/component-svg-arc
     ;;   {:stroke (colors :yellow) :fill "none"}
     ;;   (+ 45 wobble-x) (- 0 55 wobble-y)
     ;;   pulse1 0 m.PI 2]

     ;; Inner detail
     [:g {:transform (str "translate(25," (- -35) ") rotate(" rotation ")")}
      [:path {:d "M -5 0 L 0 -8 L 5 0 L 0 8 Z"
              :fill (colors :blue)
              :fill-opacity 0.9}]]

     ;; Outer ring segment with smooth fade
     (let [blink-cycle (mod t 2000)
           opacity (if (< blink-cycle 1000)
                     (* 0.8 (/ blink-cycle 1000))
                     (* 0.8 (/ (- 2000 blink-cycle) 1000)))]
       (when (> opacity 0.1)
         [components/component-svg-arc
          {:stroke (colors :red) :fill "none" :stroke-opacity opacity}
          0 0 pulse2
          (- segment-angle 0.3) (+ segment-angle 0.3) 3]))]))

(defn component-kaleidoscope-center [t]
  (let [rotation (* t 0.02)
        pulse (* 30 (+ 1 (* 0.2 (m.sin (* t 0.0012)))))]
    [:g {:transform (str "rotate(" (- rotation) ")")}
     [:circle {:cx 0 :cy 0 :r pulse
               :fill (colors :yellow)
               :fill-opacity 0.3}]
     [:circle {:cx 0 :cy 0 :r 20
               :fill (colors :blue)}]
     (for [i (range 8)
           :let [angle (* i (/ tau 8))
                 [x y] (pol2crt 0 0 15 angle)]]
       ^{:key (str "center-" i)}
       [:circle {:cx x :cy y :r 3
                 :fill (colors :yellow)}])]))

(defn component-kaleidoscope [_style]
  (let [[tl] (timeline js/Infinity)
        state (atom {:symmetry 6})]
    (fn []
      (let [t @tl
            symmetry (:symmetry @state)
            segment-angle (/ tau symmetry)]
        [:g
         ;; Clickable area
         [:circle {:cx 0 :cy 0 :r 250
                   :fill "transparent"
                   :stroke "none"
                   :style {:cursor "pointer"}
                   :on-click (partial handle-kaleidoscope-click state)}]

         ;; Kaleidoscope segments
         [:g
          (for [i (range symmetry)
                :let [angle (* i segment-angle)]]
            ^{:key (str "segment-" i)}
            [component-kaleidoscope-segment t angle])]

         ;; Center
         [component-kaleidoscope-center t]

         ;; Instructions
         [:text {:x 0 :y 270
                 :text-anchor "middle"
                 :font-size "14px"
                 :fill (colors :blue)
                 :opacity 0.7}
          (str "symmetry: " symmetry " (click to change)")]]))))

