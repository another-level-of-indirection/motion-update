(ns motion.demo-spiral-galaxy
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string]
            [motion.utils :refer [timeline pol2crt g-trans]]
            [motion.styles :refer [colors]]
            [motion.components :as components]))

(def m js/Math)
(def tau (* m.PI 2))

(defn handle-galaxy-click [galaxy-state _ev]
  (if (deref (galaxy-state :go))
    (do
      (swap! (galaxy-state :go) not)
      (reset! (galaxy-state :time) 0))
    (timeline 8000 :atoms galaxy-state)))

(defn spiral-arm-points [t arm-idx num-arms radius-max num-points]
  (let [arm-offset (* arm-idx (/ tau num-arms))
        points (for [i (range num-points)
                     :let [progress (/ i num-points)
                           r (* radius-max progress)
                           angle (+ arm-offset (* progress tau 2.5) (* t 0.0005))]]
                 (pol2crt 0 0 r angle))]
    points))

(defn component-spiral-node [_style x y size t delay color-override]
  (let [pulse (* (+ 1 (* 0.3 (m.sin (+ (* t 0.01) delay)))) size)
        opacity (if (> t delay) 1 0.3)]
    [:g (g-trans x y)
     [:circle {:cx 0 :cy 0 :r pulse 
               :fill (or color-override (colors :blue)) 
               :fill-opacity opacity
               :stroke-opacity (* opacity 0.5)}]]))

(defn component-spiral-arm [style t arm-idx num-arms radius-max num-points]
  (let [points (spiral-arm-points t arm-idx num-arms radius-max num-points)]
    [:g
     ;; Draw connecting path
     [:path {:d (str "M 0 0 L " 
                     (clojure.string/join " L " 
                                          (map (fn [[x y]] (str x " " y)) points)))
             :stroke-width "1px"
             :stroke-opacity 0.3
             :fill "none"}]
     ;; Draw nodes
     (map-indexed 
      (fn [i [x y]]
        ^{:key (str "node-" arm-idx "-" i)}
        [component-spiral-node style x y 
         (+ 3 (* 2 (/ i num-points))) 
         t 
         (* i 50)
         (if (= (mod i 3) 0) (colors :blue) nil)])
      points)]))

(defn component-central-mandala [style t]
  (let [segments 8
        rotation (* (m.min t 1000) 0.09)
        pulse (* 40 (+ 1 (* 0.15 (m.sin (* t 0.003)))))]
    [:g {:transform (str "rotate(" rotation ")")}
     ;; Inner circle
     [:circle {:cx 0 :cy 0 :r 30}]
     ;; Pulsing circle
     [:circle {:cx 0 :cy 0 :r pulse :stroke-width "3px" :fill "none"}]
     ;; Radial segments
     (for [i (range segments)
           :let [angle (* i (/ tau segments))
                 [x1 y1] (pol2crt 0 0 30 angle)
                 [x2 y2] (pol2crt 0 0 (+ pulse 10) angle)]]
       ^{:key (str "radial-" i)}
       [:path {:d (str "M " x1 " " y1 " L " x2 " " y2)
               :stroke-width "2px"}])
     ;; Outer decorative arcs
     (when (> t 500)
       [:g
        (for [i (range segments)
              :let [angle (* i (/ tau segments))
                    arc-start (- angle 0.3)
                    arc-end (+ angle 0.3)
                    arc-r (+ pulse 20)]]
          ^{:key (str "arc-" i)}
          [components/component-svg-arc 
           style 0 0 arc-r arc-start arc-end 2])])]))

(defn component-orbital-rings [_style t]
  (let [rings [{:r 100 :speed 0.002 :count 6 :size 4}
               {:r 150 :speed -0.0015 :count 8 :size 3}
               {:r 200 :speed 0.001 :count 10 :size 3}]]
    [:g
     (for [ring rings
           :let [r (:r ring)
                 speed (:speed ring)
                 count (:count ring)
                 size (:size ring)]]
       (for [i (range count)
             :let [base-angle (* i (/ tau count))
                   angle (+ base-angle (* t speed))
                   [x y] (pol2crt 0 0 r angle)
                   pulse-phase (* i 0.5)
                   pulse-size (* size (+ 1 (* 0.3 (m.sin (+ (* t 0.005) pulse-phase)))))]]
         ^{:key (str "particle-" r "-" i)}
         [:circle {:cx x :cy y :r pulse-size 
                   :fill (colors :blue)
                   :fill-opacity 0.8
                   :stroke "none"}]))]))

(defn component-expanding-polygons [_style t]
  (when (> t 1000)
    (let [progress (m.min 1 (/ (- t 1000) 2000))
          shapes [{:sides 6 :r 80 :speed 0.001}
                  {:sides 5 :r 120 :speed -0.0015}
                  {:sides 4 :r 160 :speed 0.002}]]
      [:g {:opacity (* progress 0.6)}
       (for [shape shapes
             :let [sides (:sides shape)
                   r (* (:r shape) (+ 1 (* progress 0.5)))
                   rotation (* t (:speed shape))
                   points (for [i (range sides)
                               :let [angle (+ rotation (* i (/ tau sides)))]]
                           (pol2crt 0 0 r angle))]]
         ^{:key (str "poly-" sides)}
         [:path {:d (str "M " 
                        (clojure.string/join " L " 
                                           (map (fn [[x y]] (str x " " y)) points))
                        " Z")
                 :stroke-width "2px"
                 :fill "none"}])])))

(defn component-energy-beams [_style t]
  (when (and (> t 3000) (or (< (mod t 800) 400) (> t 6000)))
    (let [beam-count 4]
      [:g {:stroke (colors :red) :stroke-width "3px"}
       (for [i (range beam-count)
             :let [angle (* i (/ tau beam-count))
                   [x y] (pol2crt 0 0 250 angle)]]
         ^{:key (str "beam-" i)}
         [:path {:d (str "M 50 0 L " x " " y)
                 :transform (str "rotate(" (* angle (/ 180 m.PI)) ")")
                 :stroke-opacity 0.7}])])))

(defn component-galaxy [style]
  (let [[tl] (timeline js/Infinity)
        galaxy-state {:time (atom 0) :go (atom false)}
        num-arms 5
        radius-max 180
        num-points 12]
    (fn []
      (let [t @tl
            galaxy-t (deref (galaxy-state :time))
            active? (deref (galaxy-state :go))]
        [:g
         ;; Clickable center area
         [:circle {:cx 0 :cy 0 :r 250 
                   :fill "transparent" 
                   :stroke "none"
                   :style {:cursor "pointer"}
                   :on-click (partial handle-galaxy-click galaxy-state)}]
         
         ;; Background rotation effect
         [:g {:transform (str "rotate(" (* t -0.01) ")")}
          (when active?
            [component-expanding-polygons style galaxy-t])]
         
         ;; Orbital rings (always rotating)
         [component-orbital-rings style t]
         
         ;; Spiral arms (animated when active)
         (when active?
           [:g
            (for [arm-idx (range num-arms)]
              ^{:key (str "arm-" arm-idx)}
              [component-spiral-arm style galaxy-t arm-idx num-arms radius-max num-points])])
         
         ;; Energy beams
         (when active?
           [component-energy-beams style galaxy-t])
         
         ;; Central mandala (always present, animates when active)
         [component-central-mandala style (if active? galaxy-t (* t 0.3))]
         
         ;; Status text
         [:text {:x 0 :y 270 
                 :text-anchor "middle"
                 :font-size "14px"
                 :fill (colors :blue)
                 :opacity 0.7}
          (if active? "click to reset" "click to activate")]]))))


