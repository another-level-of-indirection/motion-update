(ns motion.demo-wave-interference
  (:require [reagent.core :as reagent :refer [atom]]
            [motion.utils :refer [timeline g-trans]]
            [motion.styles :refer [colors]]))

(def m js/Math)
(def tau (* m.PI 2))

(defn wave-height [x y t wave-sources]
  (reduce
   (fn [sum source]
     (let [dx (- x (:x source))
           dy (- y (:y source))
           dist (m.sqrt (+ (* dx dx) (* dy dy)))
           phase (+ (* dist 0.05) (* t (:speed source)))]
       (+ sum (* (:amplitude source) (m.sin phase)))))
   0
   wave-sources))

(defn component-wave-grid [_style t wave-sources grid-size spacing]
  (let [half-grid (* grid-size 0.5)
        threshold 0.3]
    [:g
     (for [row (range grid-size)
           col (range grid-size)
           :let [x (* (- col half-grid) spacing)
                 y (* (- row half-grid) spacing)
                 h (wave-height x y t wave-sources)
                 intensity (m.abs h)
                 color (cond
                         (> h threshold) (colors :blue)
                         (< h (- threshold)) (colors :red)
                         :else "#666666")
                 opacity (m.max 0.1 (m.min 1 intensity))]]
       ^{:key (str "dot-" row "-" col)}
       [:circle {:cx x :cy y :r (+ 2 (* intensity 2))
                 :fill color
                 :fill-opacity opacity
                 :stroke "none"}])]))

(defn component-wave-source [_style x y t speed amplitude active?]
  (let [pulse-size (* amplitude 15 (+ 1 (* 0.5 (m.sin (* t speed)))))]
    [:g (g-trans x y)
     [:circle {:cx 0 :cy 0 :r 8
               :fill (if active? (colors :yellow) (colors :blue))
               :fill-opacity 0.9}]
     [:circle {:cx 0 :cy 0 :r pulse-size
               :fill "none"
               :stroke (if active? (colors :yellow) (colors :blue))
               :stroke-width "2px"
               :stroke-opacity 0.3}]]))

(defn component-wave-interference [style]
  (let [[tl] (timeline js/Infinity)
        wave-sources (atom [{:x -100 :y -100 :speed 0.003 :amplitude 1.2}
                            {:x 100 :y 100 :speed 0.004 :amplitude 1.0}
                            {:x -80 :y 120 :speed 0.0035 :amplitude 1.1}])]
    (fn []
      (let [t @tl
            sources @wave-sources]
        [:g
         ;; Wave grid
         [component-wave-grid style t sources 25 20]
         
         ;; Wave sources
         (map-indexed
          (fn [i source]
            ^{:key (str "source-" i)}
            [component-wave-source style 
             (:x source) (:y source) t 
             (:speed source) (:amplitude source) false])
          sources)
         
         ;; Title
         [:text {:x 0 :y -260
                 :text-anchor "middle"
                 :font-size "14px"
                 :fill (colors :blue)
                 :opacity 0.7}
          "wave interference pattern"]]))))

