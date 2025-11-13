(ns motion.demo-rainbow-orb
  (:require [reagent.core :as reagent :refer [atom]]
            [motion.utils :refer [timeline]]
            [motion.fx :refer [component-svg-filter-glow]]
            [motion.styles :refer [styles]]))

(defn ^export component-demo-rainbow-orb [size event-chan]
  (let [style (styles :blue-line)
        palette ["#ff4d4d" "#ff9a4d" "#fff44d" "#4dff88" "#4dd0ff" "#9b4dff"]
        n (count palette)
        base-r (fn [i] (+ 10 (* (- n i 1) 16)))
        r-max (base-r 0)
        ;; two clusters, start left and right of center
        pos1 (reagent/atom [-120 0])
        pos2 (reagent/atom [120 0])
        ;; initial velocities in pixels/sec (towards each other)
        vel1 (reagent/atom [60 20])
        vel2 (reagent/atom [-60 -20])
        colors1 (reagent/atom (vec palette))
        colors2 (reagent/atom (vec palette))
        prev-time (atom 0)
        prev-scale (atom 1)
        prev-collide1 (atom false)
        prev-collide2 (atom false)
        prev-collide-pair (atom false)
        [t] (timeline js/Infinity)]
    (fn []
      (let [[ow oh] (map #(int (/ % 2)) @size)
            time @t
            ;; cyclic time within 6s (1.0 -> 2.0 -> 0.5 -> 1.0)
            tmod (mod time 6000)
            sc (cond
                 (<= tmod 2000) (+ 1 (/ tmod 2000))                                   ;; 1.0 -> 2.0
                 (<= tmod 4000) (- 2 (* 1.5 (/ (- tmod 2000) 2000)))                  ;; 2.0 -> 0.5
                 :else (+ 0.5 (* 0.5 (/ (- tmod 4000) 2000))))                        ;; 0.5 -> 1.0
            dt (- time @prev-time)]
        ;; update physics once per frame (when time advanced)
        (when (> dt 0)
          (reset! prev-time time)
          (let [R (* r-max sc)]
            ;; cluster 1 integration
            (let [[x1 y1] @pos1
                  [vx1 vy1] @vel1
                  nx1 (+ x1 (* vx1 (/ dt 1000.0)))
                  ny1 (+ y1 (* vy1 (/ dt 1000.0)))
                  collide-right-1? (> (+ nx1 R) ow)
                  collide-left-1? (< (- nx1 R) (- ow))
                  collide-bottom-1? (> (+ ny1 R) oh)
                  collide-top-1? (< (- ny1 R) (- oh))
                  vx1' (cond collide-right-1? (- (js/Math.abs vx1))
                             collide-left-1? (js/Math.abs vx1)
                             :else vx1)
                  vy1' (cond collide-bottom-1? (- (js/Math.abs vy1))
                             collide-top-1? (js/Math.abs vy1)
                             :else vy1)
                  x1' (cond collide-right-1? (- ow R)
                            collide-left-1? (+ (- ow) R)
                            :else nx1)
                  y1' (cond collide-bottom-1? (- oh R)
                            collide-top-1? (+ (- oh) R)
                            :else ny1)]
              (reset! vel1 [vx1' vy1'])
              (reset! pos1 [x1' y1'])
              (let [collided1? (or collide-right-1? collide-left-1? collide-top-1? collide-bottom-1?)]
                (when (and collided1? (not @prev-collide1))
                  (swap! colors1 (fn [cols] (vec (concat (subvec cols 1) (subvec cols 0 1))))))
                (reset! prev-collide1 collided1?)))

            ;; cluster 2 integration
            (let [[x2 y2] @pos2
                  [vx2 vy2] @vel2
                  nx2 (+ x2 (* vx2 (/ dt 1000.0)))
                  ny2 (+ y2 (* vy2 (/ dt 1000.0)))
                  collide-right-2? (> (+ nx2 R) ow)
                  collide-left-2? (< (- nx2 R) (- ow))
                  collide-bottom-2? (> (+ ny2 R) oh)
                  collide-top-2? (< (- ny2 R) (- oh))
                  vx2' (cond collide-right-2? (- (js/Math.abs vx2))
                             collide-left-2? (js/Math.abs vx2)
                             :else vx2)
                  vy2' (cond collide-bottom-2? (- (js/Math.abs vy2))
                             collide-top-2? (js/Math.abs vy2)
                             :else vy2)
                  x2' (cond collide-right-2? (- ow R)
                            collide-left-2? (+ (- ow) R)
                            :else nx2)
                  y2' (cond collide-bottom-2? (- oh R)
                            collide-top-2? (+ (- oh) R)
                            :else ny2)]
              (reset! vel2 [vx2' vy2'])
              (reset! pos2 [x2' y2'])
              (let [collided2? (or collide-right-2? collide-left-2? collide-top-2? collide-bottom-2?)]
                (when (and collided2? (not @prev-collide2))
                  (swap! colors2 (fn [cols] (vec (concat (subvec cols 1) (subvec cols 0 1))))))
                (reset! prev-collide2 collided2?)))

            ;; now handle inter-cluster collision (treat clusters as circles with radius R)
            (let [[x1 y1] @pos1
                  [x2 y2] @pos2
                  dx (- x2 x1)
                  dy (- y2 y1)
                  dist (js/Math.sqrt (+ (* dx dx) (* dy dy)))
                  collided-pair? (<= dist (* 2 R))]
              (when (and collided-pair? (not @prev-collide-pair))
                (let [nx (if (zero? dist) 1 (/ dx dist))
                      ny (if (zero? dist) 0 (/ dy dist))
                      [vx1 vy1] @vel1
                      [vx2 vy2] @vel2
                      vrelx (- vx1 vx2)
                      vrely (- vy1 vy2)
                      rel (+ (* vrelx nx) (* vrely ny))]
                  (when (< rel 0)
                    (let [p rel
                          vx1' (- vx1 (* p nx))
                          vy1' (- vy1 (* p ny))
                          vx2' (+ vx2 (* p nx))
                          vy2' (+ vy2 (* p ny))]
                      (reset! vel1 [vx1' vy1'])
                      (reset! vel2 [vx2' vy2'])))
                  ;; clamp positions
                  (let [overlap (- (* 2 R) dist)
                        sep (/ overlap 2)
                        x1' (- x1 (* sep nx))
                        y1' (- y1 (* sep ny))
                        x2' (+ x2 (* sep nx))
                        y2' (+ y2 (* sep ny))]
                    (reset! pos1 [x1' y1'])
                    (reset! pos2 [x2' y2']))
                  ;; rotate colors for both on inter-cluster bounce
                  (swap! colors1 (fn [cols] (vec (concat (subvec cols 1) (subvec cols 0 1)))))
                  (swap! colors2 (fn [cols] (vec (concat (subvec cols 1) (subvec cols 0 1)))))))
              (reset! prev-collide-pair collided-pair?))))

        (reset! prev-scale sc)

        ;; render both clusters
        (let [[x1 y1] @pos1
              [x2 y2] @pos2]
          [:g style
           [:defs (component-svg-filter-glow)]
           ;; cluster 1
           [:g {:transform (str "translate(" x1 "," y1 ") scale(" sc ")")}
            (doall
             (map-indexed
              (fn [i col]
                ^{:key (str "c1-" i)}
                [:circle {:cx 0 :cy 0 :r (base-r i) :fill col :opacity 0.92}])
              @colors1))]
           ;; cluster 2
           [:g {:transform (str "translate(" x2 "," y2 ") scale(" sc ")")}
            (doall
             (map-indexed
              (fn [i col]
                ^{:key (str "c2-" i)}
                [:circle {:cx 0 :cy 0 :r (base-r i) :fill col :opacity 0.92}])
              @colors2))]])))))
