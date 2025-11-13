(ns motion.demo-hex-match
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string]
            [motion.utils :refer [g-trans hex-pos hexagon]]
            [motion.styles :refer [colors]]))

(def m js/Math)

(defn create-hex-grid []
  (let [color-palette [(colors :blue) (colors :red) (colors :yellow) "#00D9FF" "#FF6B9D" "#A0FF88"]
        colors-list (shuffle (concat color-palette color-palette color-palette color-palette))]
    (into {}
          (map-indexed
           (fn [idx [c r]]
             [[c r] {:color (nth colors-list idx)
                     :revealed false
                     :matched false}])
           (for [c (range -2 3) r (range -2 2)] [c r])))))

(defn component-hex-tile [style hex-data pos size revealed? matched? selected?]
  (let [[x y] (hex-pos size (first pos) (second pos))
        color (:color hex-data)
        opacity (cond
                  matched? 0.3
                  revealed? 1.0
                  :else 0.1)
        scale (cond
                selected? 0.95
                revealed? 1.0
                :else 0.9)]
    [:g (g-trans x y)
     [:g {:transform (str "scale(" scale ")")}
      [:path {:d (str "M " (clojure.string/join " L " (hexagon size false)) " Z")
              :fill color
              :fill-opacity opacity
              :stroke (:stroke style)
              :stroke-width 2
              :style {:cursor (if matched? "default" "pointer")}}]
      (when (and revealed? (not matched?))
        [:circle {:cx 0 :cy 0 :r 8
                  :fill color
                  :stroke "white"
                  :stroke-width 2}])
      (when matched?
        [:g
         [:line {:x1 -15 :y1 0 :x2 15 :y2 0 :stroke (:stroke style) :stroke-width 2 :opacity 0.5}]
         [:line {:x1 0 :y1 -15 :x2 0 :y2 15 :stroke (:stroke style) :stroke-width 2 :opacity 0.5}]])]]))

(defn check-match [game-state first-pick second-pick]
  (let [first-color (get-in @game-state [:grid first-pick :color])
        second-color (get-in @game-state [:grid second-pick :color])]
    (= first-color second-color)))

(defn handle-hex-click [game-state pos]
  (let [hex (get-in @game-state [:grid pos])
        current-picks @(:picks @game-state)
        matched? (:matched hex)
        revealed? (:revealed hex)]
    (when (and (not matched?) (not revealed?) (< (count current-picks) 2))
      ;; Reveal the hex
      (swap! game-state assoc-in [:grid pos :revealed] true)
      (swap! (:picks @game-state) conj pos)
      
      ;; If this is the second pick, check for match
      (when (= (count @(:picks @game-state)) 2)
        (let [[first-pick second-pick] @(:picks @game-state)]
          (if (check-match game-state first-pick second-pick)
            ;; Match found!
            (do
              (swap! (:matches @game-state) inc)
              (swap! game-state assoc-in [:grid first-pick :matched] true)
              (swap! game-state assoc-in [:grid second-pick :matched] true)
              (js/setTimeout #(reset! (:picks @game-state) []) 500))
            ;; No match, hide after delay
            (js/setTimeout 
             (fn []
               (swap! game-state assoc-in [:grid first-pick :revealed] false)
               (swap! game-state assoc-in [:grid second-pick :revealed] false)
               (reset! (:picks @game-state) []))
             800)))))))

(defn component-hex-match [style]
  (let [game-state (atom {:grid (create-hex-grid)
                          :picks (atom [])
                          :matches (atom 0)
                          :moves (atom 0)})
        size 35
        reset-game (fn []
                     (reset! game-state {:grid (create-hex-grid)
                                         :picks (atom [])
                                         :matches (atom 0)
                                         :moves (atom 0)}))]
    (fn []
      (let [total-pairs 12
            matches @(:matches @game-state)
            game-won? (= matches total-pairs)
            current-picks @(:picks @game-state)]
        [:g
         ;; Hex grid
         (for [[[c r] hex-data] (:grid @game-state)]
           ^{:key (str "hex-" c "-" r)}
           [:g {:on-click #(do
                             (when (empty? current-picks)
                               (swap! (:moves @game-state) inc))
                             (handle-hex-click game-state [c r]))}
            [component-hex-tile style hex-data [c r] size 
             (:revealed hex-data) 
             (:matched hex-data)
             (some #{[c r]} current-picks)]])
         
         ;; Score display
         [:g {:transform "translate(0, -180)"}
          [:text {:x 0 :y 0
                  :text-anchor "middle"
                  :font-size "16px"
                  :fill (colors :blue)
                  :font-weight "bold"}
           (str "Matches: " matches "/" total-pairs)]
          [:text {:x 0 :y 20
                  :text-anchor "middle"
                  :font-size "12px"
                  :fill (colors :blue)
                  :opacity 0.7}
           (str "Moves: " @(:moves @game-state))]]
         
         ;; Win message and reset button
         (when game-won?
           [:g
            [:rect {:x -80 :y -50 :width 160 :height 80
                    :fill "#000000"
                    :fill-opacity 0.9
                    :stroke (colors :blue)
                    :stroke-width 3
                    :rx 10}]
            [:text {:x 0 :y -20
                    :text-anchor "middle"
                    :font-size "20px"
                    :fill (colors :yellow)
                    :font-weight "bold"}
             "YOU WIN!"]
            [:text {:x 0 :y 0
                    :text-anchor "middle"
                    :font-size "12px"
                    :fill (colors :blue)}
             (str @(:moves @game-state) " moves")]
            [:g {:on-click reset-game
                 :style {:cursor "pointer"}}
             [:rect {:x -40 :y 10 :width 80 :height 25
                     :fill (colors :blue)
                     :rx 5}]
             [:text {:x 0 :y 28
                     :text-anchor "middle"
                     :font-size "14px"
                     :fill "#000000"
                     :font-weight "bold"}
              "Play Again"]]])
         
         ;; Instructions
         [:text {:x 0 :y 200
                 :text-anchor "middle"
                 :font-size "12px"
                 :fill (colors :blue)
                 :opacity 0.7}
          "Match pairs of colors by clicking hexes"]]))))

