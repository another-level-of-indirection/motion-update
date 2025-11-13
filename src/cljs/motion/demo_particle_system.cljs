(ns motion.demo-particle-system
  (:require [reagent.core :as reagent :refer [atom]]
            [motion.utils :refer [timeline g-trans]]
            [motion.styles :refer [colors]]))

(def m js/Math)
(def tau (* m.PI 2))

(defn create-particle [idx]
  (let [angle (* (m.random) tau)
        speed (+ 0.5 (* (m.random) 1.5))
        distance (* (m.random) 50)]
    {:id idx
     :x (* distance (m.cos angle))
     :y (* distance (m.sin angle))
     :vx (* speed (m.cos angle))
     :vy (* speed (m.sin angle))
     :life 1.0
     :size (+ 2 (* (m.random) 4))
     :color (rand-nth [(colors :blue) (colors :red) (colors :yellow)])
     :rotation (* (m.random) tau)
     :rotation-speed (- (* (m.random) 0.1) 0.05)}))

(defn update-particle [particle dt _boundary]
  (let [new-x (+ (:x particle) (* (:vx particle) dt))
        new-y (+ (:y particle) (* (:vy particle) dt))
        new-life (- (:life particle) (* 0.0003 dt))
        friction 0.998
        gravity 0.02]
    (assoc particle
           :x new-x
           :y (+ new-y (* gravity dt))
           :vx (* (:vx particle) friction)
           :vy (+ (* (:vy particle) friction) (* gravity dt))
           :life new-life
           :rotation (+ (:rotation particle) (* (:rotation-speed particle) dt)))))

(defn component-particle [particle]
  (let [{:keys [x y size color life rotation]} particle
        opacity (* life 0.8)]
    [:g (merge (g-trans x y) {:transform (str "translate(" x "," y ") rotate(" (* rotation (/ 180 m.PI)) ")")})
     [:circle {:cx 0 :cy 0 :r size
               :fill color
               :fill-opacity opacity}]
     [:circle {:cx 0 :cy 0 :r (* size 1.5)
               :fill "none"
               :stroke color
               :stroke-width "1px"
               :stroke-opacity (* opacity 0.3)}]]))

(defn component-emitter [_style x y]
  [:g (g-trans x y)
   [:circle {:cx 0 :cy 0 :r 12
             :fill (colors :yellow)
             :filter "url(#glowfilter)"}]
   [:path {:d "M -6 -6 L 6 6 M 6 -6 L -6 6"
           :stroke (colors :background)
           :stroke-width "2px"}]])

(defn component-particle-system [style]
  (let [[tl] (timeline js/Infinity)
        particles (atom [])
        last-spawn (atom 0)
        next-id (atom 0)
        spawn-interval 50
        max-particles 150]
    (fn []
      (let [t @tl
            dt 16]
        ;; Spawn new particles
        (when (> (- t @last-spawn) spawn-interval)
          (reset! last-spawn t)
          (when (< (count @particles) max-particles)
            (swap! particles conj (create-particle @next-id))
            (swap! next-id inc)))
        
        ;; Update particles
        (reset! particles
                (->> @particles
                     (map #(update-particle % dt 250))
                     (filter #(> (:life %) 0))
                     vec))
        
        [:g
         ;; Particles
         (for [p @particles]
           ^{:key (:id p)}
           [component-particle p])
         
         ;; Emitter
         [component-emitter style 0 -150]
         
         ;; Stats
         [:text {:x 0 :y 260
                 :text-anchor "middle"
                 :font-size "12px"
                 :fill (colors :blue)
                 :opacity 0.5}
          (str (count @particles) " particles")]]))))

