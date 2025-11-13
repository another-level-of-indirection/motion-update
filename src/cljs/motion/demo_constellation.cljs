(ns motion.demo-constellation
  (:require [reagent.core :as reagent :refer [atom]]
            [motion.utils :refer [timeline g-trans]]
            [motion.styles :refer [colors]]))

(def m js/Math)
(def tau (* m.PI 2))

(defn create-star []
  (let [angle (* (m.random) tau)
        distance (+ 80 (* (m.random) 150))]
    {:x (* distance (m.cos angle))
     :y (* distance (m.sin angle))
     :vx (- (* (m.random) 0.3) 0.15)
     :vy (- (* (m.random) 0.3) 0.15)
     :size (+ 2 (* (m.random) 4))
     :twinkle-phase (* (m.random) tau)
     :twinkle-speed (+ 0.001 (* (m.random) 0.003))}))

(defn update-star [star dt]
  (let [new-x (+ (:x star) (* (:vx star) dt))
        new-y (+ (:y star) (* (:vy star) dt))
        dist (m.sqrt (+ (* new-x new-x) (* new-y new-y)))]
    ;; Wrap around or bounce back
    (if (> dist 250)
      (let [angle (m.atan2 new-y new-x)]
        (assoc star
               :x (* 240 (m.cos angle))
               :y (* 240 (m.sin angle))
               :vx (- (:vx star))
               :vy (- (:vy star))))
      (assoc star :x new-x :y new-y))))

(defn distance [star1 star2]
  (let [dx (- (:x star1) (:x star2))
        dy (- (:y star1) (:y star2))]
    (m.sqrt (+ (* dx dx) (* dy dy)))))

(defn component-star [star t]
  (let [{:keys [x y size twinkle-phase twinkle-speed]} star
        brightness (+ 0.5 (* 0.5 (m.sin (+ twinkle-phase (* t twinkle-speed)))))
        glow-size (* size (+ 1.5 brightness))]
    [:g (g-trans x y)
     [:circle {:cx 0 :cy 0 :r glow-size
               :fill (colors :blue)
               :fill-opacity (* 0.2 brightness)}]
     [:circle {:cx 0 :cy 0 :r size
               :fill (colors :blue)
               :fill-opacity (+ 0.6 (* 0.4 brightness))}]
     [:circle {:cx 0 :cy 0 :r (/ size 2)
               :fill (colors :yellow)
               :fill-opacity brightness}]]))

(defn component-constellation-line [star1 star2 dist max-dist]
  (let [opacity (m.max 0 (- 1 (/ dist max-dist)))]
    [:line {:x1 (:x star1) :y1 (:y star1)
            :x2 (:x star2) :y2 (:y star2)
            :stroke (colors :blue)
            :stroke-width "1px"
            :stroke-opacity (* opacity 0.4)}]))

(defn component-constellation [_style]
  (let [[tl] (timeline js/Infinity)
        stars (atom (vec (repeatedly 30 create-star)))
        connection-threshold 80]
    (fn []
      (let [t @tl
            dt 16]
        ;; Update star positions
        (swap! stars #(mapv (fn [star] (update-star star dt)) %))
        
        (let [star-list @stars]
          [:g
           ;; Connection lines
           [:g
            (for [i (range (count star-list))
                  j (range (inc i) (count star-list))
                  :let [s1 (nth star-list i)
                        s2 (nth star-list j)
                        d (distance s1 s2)]
                  :when (< d connection-threshold)]
              ^{:key (str "line-" i "-" j)}
              [component-constellation-line s1 s2 d connection-threshold])]
           
           ;; Stars
           (map-indexed
            (fn [i star]
              ^{:key (str "star-" i)}
              [component-star star t])
            star-list)
           
           ;; Center ornament
           [:g
            [:circle {:cx 0 :cy 0 :r 40
                      :fill "none"
                      :stroke (colors :blue)
                      :stroke-width "2px"
                      :stroke-opacity 0.3
                      :stroke-dasharray "5,5"
                      :transform (str "rotate(" (* t 0.02) ")")}]
            [:circle {:cx 0 :cy 0 :r 8
                      :fill (colors :blue)
                      :fill-opacity 0.5}]]
           
           ;; Title
           [:text {:x 0 :y 270
                   :text-anchor "middle"
                   :font-size "14px"
                   :fill (colors :blue)
                   :opacity 0.7}
            "dynamic constellation"]])))))

