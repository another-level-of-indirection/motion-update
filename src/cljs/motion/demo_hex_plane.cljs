(ns motion.demo-hex-plane
  (:require [reagent.core :as reagent :refer [atom]]
            [motion.utils :refer [g-trans hex-pos]]
            [motion.demo-curved-path :refer [component-svg-hexagon component-svg-rounded-hexagon]]))

(defn component-svg-hex-plane [style-1 style-2]
  (let [selected (atom nil)
        clicked (atom #{})
        size 40]
    (fn []
      [:g
       (doall
        (for [c (range -4 5) r (range -3 3)]
          (let [[x y] (hex-pos size c r)
                selected? (= @selected [c r])]
            [(if selected? component-svg-rounded-hexagon component-svg-hexagon)
             (assoc (if selected? style-1 style-2)
                    :key (str "hex-" c "-" r)
                    :on-mouse-over (fn [ev] (reset! selected [c r]) nil)
                    :on-mouse-out (fn [ev] (reset! selected nil) nil)
                    :on-click (fn [ev]
                                (reset! selected [c r])
                                (swap! clicked conj [c r])
                                nil))
             x y (* size 0.95)])))

       ;; Red circles on clicked hexes
       (doall
        (for [coords @clicked]
          (let [[c r] coords
                [x y] (hex-pos size c r)]
            [:circle {:key (str "clicked-" c "-" r)
                      :cx x :cy y :r 4
                      :fill "#E63946"
                      :stroke "#FF0000"
                      :stroke-width 1
                      :style {:pointer-events "none"}}])))

       ;; Tooltip showing hex coordinates
       (when @selected
         (let [[c r] @selected
               [x y] (hex-pos size c r)]
           [:g {:key "tooltip"}
            ;; Background for tooltip
            [:rect {:x (- x 25) :y (- y 20)
                    :width 50 :height 16
                    :rx 3
                    :fill "#000"
                    :fill-opacity 0.8
                    :stroke (:stroke style-1)
                    :stroke-width 1}]
            ;; Tooltip text
            [:text {:x x :y (- y 9)
                    :text-anchor "middle"
                    :fill "#DDD"
                    :font-family "monospace"
                    :font-size "10px"
                    :style {:pointer-events "none"}}
             (str "[" c "," r "]")]]))])))
