(ns motion.demo-hex-lights
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string]
            [motion.utils :refer [timeline g-trans hex-pos hexagon]]
            [motion.styles :refer [colors]]))

(def m js/Math)

(defn get-neighbors [[c r]]
  [[(inc c) r]
   [(dec c) r]
   [c (inc r)]
   [c (dec r)]
   [(inc c) (dec r)]
   [(dec c) (inc r)]])

(defn create-hex-grid [pattern]
  (let [all-coords (for [c (range -2 3) r (range -2 2)] [c r])]
    (into {}
          (map-indexed
           (fn [_idx pos]
             [pos (if (contains? pattern pos) true false)])
           all-coords))))

(defn generate-random-pattern []
  (let [coords (for [c (range -2 3) r (range -2 2)] [c r])
        num-on (+ 5 (rand-int 10))]
    (set (take num-on (shuffle coords)))))

(defn toggle-hex [grid pos]
  (let [positions (cons pos (get-neighbors pos))]
    (reduce
     (fn [g p]
       (if (contains? g p)
         (update g p not)
         g))
     grid
     positions)))

(defn all-lights-off? [grid]
  (every? false? (vals grid)))

(defn count-lights-on [grid]
  (count (filter true? (vals grid))))

(defn component-hex-tile [style pos size is-on? hover? animation-time]
  (let [[x y] (hex-pos size (first pos) (second pos))
        pulse (+ 1 (* 0.1 (m.sin (/ animation-time 200))))
        glow-size (if is-on? (* size 1.2 pulse) size)
        opacity (if is-on? 0.9 0.15)
        inner-opacity (if is-on? 1.0 0.3)
        stroke-width (cond
                       hover? 3
                       is-on? 2
                       :else 1)
        fill-color (if is-on? (colors :blue) "#1a1a1a")]
    [:g (g-trans x y)
     ;; Glow effect when on
     (when is-on?
       [:circle {:cx 0 :cy 0 :r glow-size
                 :fill (colors :blue)
                 :opacity 0.2
                 :style {:pointer-events "none"}}])
     
     ;; Main hex
     [:path {:d (str "M " (clojure.string/join " L " (hexagon size false)) " Z")
             :fill fill-color
             :fill-opacity opacity
             :stroke (if hover? (colors :yellow) (:stroke style))
             :stroke-width stroke-width
             :style {:cursor "pointer"}}]
     
     ;; Inner decoration
     (when is-on?
       [:g
        [:circle {:cx 0 :cy 0 :r 8
                  :fill (colors :yellow)
                  :opacity inner-opacity}]
        [:circle {:cx 0 :cy 0 :r 4
                  :fill "white"
                  :opacity inner-opacity}]])
     
     ;; Hover indicator
     (when (and hover? (not is-on?))
       [:circle {:cx 0 :cy 0 :r 4
                 :fill (colors :yellow)
                 :opacity 0.5}])]))

(defn component-hex-lights [style]
  (let [[tl] (timeline js/Infinity)
        game-state (atom {:grid (create-hex-grid (generate-random-pattern))
                          :moves 0
                          :hover nil
                          :best-score nil})
        
        handle-click (fn [pos]
                       (swap! game-state update :grid toggle-hex pos)
                       (swap! game-state update :moves inc))
        
        reset-game (fn []
                     (let [current-best (:best-score @game-state)]
                       (reset! game-state {:grid (create-hex-grid (generate-random-pattern))
                                           :moves 0
                                           :hover nil
                                           :best-score current-best})))]
    (fn []
      (let [t @tl
            {:keys [grid moves hover best-score]} @game-state
            lights-on (count-lights-on grid)
            game-won? (all-lights-off? grid)
            new-best? (and game-won? (or (nil? best-score) (< moves best-score)))]
        
        ;; Update best score
        (when new-best?
          (swap! game-state assoc :best-score moves))
        
        [:g
         ;; Hex grid
         (for [[pos is-on?] grid]
           (let [hover? (= hover pos)]
             ^{:key (str "hex-" (first pos) "-" (second pos))}
             [:g {:on-click #(when (not game-won?) (handle-click pos))
                  :on-mouse-enter #(swap! game-state assoc :hover pos)
                  :on-mouse-leave #(swap! game-state assoc :hover nil)}
              [component-hex-tile style pos 32 is-on? hover? t]]))
         
         ;; Status display
         [:g {:transform "translate(0, -140)"}
          [:text {:x 0 :y 0
                  :text-anchor "middle"
                  :font-size "16px"
                  :fill (colors :blue)
                  :font-weight "bold"}
           (str "Lights On: " lights-on)]
          [:text {:x 0 :y 18
                  :text-anchor "middle"
                  :font-size "12px"
                  :fill (colors :blue)
                  :opacity 0.7}
           (str "Moves: " moves)]
          (when best-score
            [:text {:x 0 :y 33
                    :text-anchor "middle"
                    :font-size "11px"
                    :fill (colors :yellow)
                    :opacity 0.8}
             (str "Best: " best-score)])]
         
         ;; Win display
         (when game-won?
           [:g
            [:rect {:x -90 :y -50 :width 180 :height 90
                    :fill "#000000"
                    :fill-opacity 0.9
                    :stroke (if new-best? (colors :yellow) (colors :blue))
                    :stroke-width 3
                    :rx 10}]
            [:text {:x 0 :y -25
                    :text-anchor "middle"
                    :font-size "20px"
                    :fill (if new-best? (colors :yellow) (colors :blue))
                    :font-weight "bold"}
             (if new-best? "NEW BEST!" "ALL CLEAR!")]
            [:text {:x 0 :y -5
                    :text-anchor "middle"
                    :font-size "14px"
                    :fill (colors :blue)}
             (str "Solved in " moves " moves")]
            [:g {:on-click reset-game
                 :style {:cursor "pointer"}}
             [:rect {:x -45 :y 10 :width 90 :height 25
                     :fill (colors :blue)
                     :rx 5}]
             [:text {:x 0 :y 28
                     :text-anchor "middle"
                     :font-size "14px"
                     :fill "#000000"
                     :font-weight "bold"}
              "New Puzzle"]]])
         
         ;; Instructions
         [:text {:x 0 :y 165
                 :text-anchor "middle"
                 :font-size "11px"
                 :fill (colors :blue)
                 :opacity 0.7}
          "Click hexes to toggle them and neighbors. Turn all lights off!"]]))))

