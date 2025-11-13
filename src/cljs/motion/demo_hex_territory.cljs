(ns motion.demo-hex-territory
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string]
            [motion.utils :refer [g-trans hex-pos hexagon]]
            [motion.styles :refer [colors]]))

(def m js/Math)

(defn get-neighbors [[c r]]
  [[(inc c) r]
   [(dec c) r]
   [c (inc r)]
   [c (dec r)]
   [(inc c) (dec r)]
   [(dec c) (inc r)]])

(defn count-territory [grid player]
  (count (filter #(= (second %) player) grid)))

(defn create-hex-grid []
  (into {}
        (for [c (range -3 4) r (range -3 3)]
          [[c r] nil])))

(defn can-place? [grid pos current-player]
  (let [hex-owner (get grid pos)
        neighbors (get-neighbors pos)
        has-friendly-neighbor? (some #(= (get grid %) current-player) neighbors)]
    (and (nil? hex-owner)
         (or (= current-player :player1) ;; Player 1 can always place
             has-friendly-neighbor?)))) ;; Player 2 needs adjacent hex

(defn get-longest-path [grid player pos visited]
  (if (or (visited pos) (not= (get grid pos) player))
    0
    (let [new-visited (conj visited pos)
          neighbors (get-neighbors pos)
          neighbor-paths (map #(get-longest-path grid player % new-visited) neighbors)]
      (+ 1 (apply max 0 neighbor-paths)))))

(defn component-hex-tile [style pos size owner current-player can-place? hover?]
  (let [[x y] (hex-pos size (first pos) (second pos))
        fill-color (cond
                     (= owner :player1) (colors :blue)
                     (= owner :player2) (colors :red)
                     :else "#1a1a1a")
        opacity (cond
                  owner 0.9
                  (and can-place? hover?) 0.4
                  can-place? 0.2
                  :else 0.1)
        stroke-width (cond
                       (and can-place? hover?) 3
                       can-place? 2
                       :else 1)
        stroke-color (cond
                       (and can-place? hover?) (if (= current-player :player1)
                                                  (colors :blue)
                                                  (colors :red))
                       :else (:stroke style))]
    [:g (g-trans x y)
     [:path {:d (str "M " (clojure.string/join " L " (hexagon size false)) " Z")
             :fill fill-color
             :fill-opacity opacity
             :stroke stroke-color
             :stroke-width stroke-width
             :style {:cursor (if can-place? "pointer" "default")}}]
     (when owner
       [:circle {:cx 0 :cy 0 :r 6
                 :fill fill-color
                 :stroke "white"
                 :stroke-width 2}])]))

(defn component-hex-territory [style]
  (let [game-state (atom {:grid (create-hex-grid)
                          :current-player :player1
                          :p1-score 0
                          :p2-score 0
                          :hover nil})
        size 30
        
        handle-click (fn [pos]
                       (let [{:keys [grid current-player]} @game-state]
                         (when (can-place? grid pos current-player)
                           (swap! game-state update :grid assoc pos current-player)
                           (let [new-grid (:grid @game-state)
                                 p1-score (count-territory new-grid :player1)
                                 p2-score (count-territory new-grid :player2)]
                             (swap! game-state assoc
                                    :p1-score p1-score
                                    :p2-score p2-score
                                    :current-player (if (= current-player :player1) :player2 :player1))))))
        
        reset-game (fn []
                     (reset! game-state {:grid (create-hex-grid)
                                         :current-player :player1
                                         :p1-score 0
                                         :p2-score 0
                                         :hover nil}))]
    (fn []
      (let [{:keys [grid current-player p1-score p2-score hover]} @game-state
            total-hexes (count grid)
            filled-hexes (+ p1-score p2-score)
            game-over? (= filled-hexes total-hexes)]
        [:g
         ;; Hex grid
         (for [[pos owner] grid]
           (let [can-place? (and (not game-over?) (can-place? grid pos current-player))
                 hover? (= hover pos)]
             ^{:key (str "hex-" (first pos) "-" (second pos))}
             [:g {:on-click #(handle-click pos)
                  :on-mouse-enter #(swap! game-state assoc :hover pos)
                  :on-mouse-leave #(swap! game-state assoc :hover nil)}
              [component-hex-tile style pos size owner current-player can-place? hover?]]))
         
         ;; Score display
         [:g {:transform "translate(-80, -160)"}
          [:circle {:cx 0 :cy 0 :r 10 :fill (colors :blue) :opacity 0.8}]
          [:text {:x 20 :y 5
                  :font-size "16px"
                  :fill (colors :blue)
                  :font-weight "bold"}
           (str "Player 1: " p1-score)]]
         
         [:g {:transform "translate(80, -160)"}
          [:circle {:cx 0 :cy 0 :r 10 :fill (colors :red) :opacity 0.8}]
          [:text {:x -20 :y 5
                  :text-anchor "end"
                  :font-size "16px"
                  :fill (colors :red)
                  :font-weight "bold"}
           (str "Player 2: " p2-score)]]
         
         ;; Current player indicator
         (when (not game-over?)
           [:g {:transform "translate(0, -180)"}
            [:text {:x 0 :y 0
                    :text-anchor "middle"
                    :font-size "14px"
                    :fill (if (= current-player :player1) (colors :blue) (colors :red))
                    :opacity 0.8}
             (str (if (= current-player :player1) "Player 1" "Player 2") " Turn")]])
         
         ;; Game over display
         (when game-over?
           (let [winner (cond
                          (> p1-score p2-score) "Player 1 Wins!"
                          (> p2-score p1-score) "Player 2 Wins!"
                          :else "Draw!")
                 winner-color (cond
                                (> p1-score p2-score) (colors :blue)
                                (> p2-score p1-score) (colors :red)
                                :else (colors :yellow))]
             [:g
              [:rect {:x -100 :y -50 :width 200 :height 80
                      :fill "#000000"
                      :fill-opacity 0.9
                      :stroke winner-color
                      :stroke-width 3
                      :rx 10}]
              [:text {:x 0 :y -20
                      :text-anchor "middle"
                      :font-size "20px"
                      :fill winner-color
                      :font-weight "bold"}
               winner]
              [:text {:x 0 :y 0
                      :text-anchor "middle"
                      :font-size "14px"
                      :fill (colors :blue)
                      :opacity 0.8}
               (str p1-score " - " p2-score)]
              [:g {:on-click reset-game
                   :style {:cursor "pointer"}}
               [:rect {:x -45 :y 10 :width 90 :height 25
                       :fill winner-color
                       :rx 5}]
               [:text {:x 0 :y 28
                       :text-anchor "middle"
                       :font-size "14px"
                       :fill "#000000"
                       :font-weight "bold"}
                "New Game"]]]))
         
         ;; Instructions
         [:text {:x 0 :y 180
                 :text-anchor "middle"
                 :font-size "11px"
                 :fill (colors :blue)
                 :opacity 0.7}
          "Claim hexes! P2 must expand from their territory"]]))))

