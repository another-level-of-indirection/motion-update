(ns motion.demo-walker
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [put! chan timeout <!] :as async]
            [motion.utils :refer [timeline svg-path partial-path svg-arc hex-pos]]
            [motion.demo-curved-path :refer [component-svg-hexagon component-svg-rounded-hexagon]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def m js/Math)

(defn arms-1 []
  [:g
   [:path {:d "M -7.5 -59.5 L -15.5 -25.5"}]
   [:path {:d "M 7.5 -59.5 L 15.5 -25.5"}]])

(defn arms-2 []
  [:g
   [:path {:d "M -7.5 -59.5 L -5.5 -25.5"}]
   [:path {:d "M 7.5 -59.5 L 5.5 -25.5"}]])

(defn component-walker [style-1 style-2 position timeline]
  (let [helmet-path (str "M 15.5 -60.5 L -15.5 -60.5"
                         " "
                         (js/roundPathCorners "M -15.5 -60.5 L -15.5 -95.5 L 15.5 -95.5 L 15.5 -60.5" 15 false))
        helmet-shine (js/roundPathCorners "M -11.5 -80.5 L -11.5 -91.5 L 0 -91.5" 15 false)]
    (fn []
      (let [frame (mod (int (/ @timeline 150)) 2)
            legs-frame frame
            arms-frame frame]
        [:g (merge style-1 {:stroke-width "2px" :transform (str "translate(" (first @position) "," (second @position) ")")})
           ; head
         [:g
          [:path {:d helmet-path :fill "#3A586C"}]
          [:path {:d helmet-shine :fill "none" :stroke "#383838" :stroke-width "3px"}]]
           ; arms
         (if (= @timeline 0)
           (arms-1)
           (case arms-frame
             0 [arms-1]
             1 [arms-2]))
           ; body
         [:path {:d "M -7.5 -30.5 L -7.5 -60.5 L 7.5 -60.5 L 7.5 -30.5"
                 :fill "url(#hatch)"}]
           ; legs
         (if (= @timeline 0)
           [:path {:d "M -7.5 0 L -7.5 -30.5 L 7.5 -30.5 L 7.5 0"}]
           (case legs-frame
             0 [:path {:d "M -5.5 0 L -7.5 -30.5 L 7.5 -30.5 L 5.5 0"}]
             1 [:path {:d "M -10.5 0 L -7.5 -30.5 L 7.5 -30.5 L 10.5 0"}]))]))))

(defn pos-to-hex [size x y]
  "Convert x,y position to hex coordinates [c r]"
  (let [sqrt3 (.sqrt m 3)
        q (/ (* (/ 2 3) x) size)
        r (/ (- (/ y (* size sqrt3 0.5)) (* (/ x size) (/ 1 sqrt3))) (/ 2 sqrt3))
        c (.round m q)
        rr (.round m r)]
    [c rr]))

(defn component-walker-hex-map [style-1 style-2 style-3 click-channel traversed-hexes]
  (let [selected (atom nil)
        size 50]
    (fn []
      [:g {:stroke "none"}
       (doall
        (for [c (range -4 5) r (range -3 3)]
          (let [[x y] (hex-pos size c r)
                selected? (= @selected [c r])
                traversed? (contains? @traversed-hexes [c r])
                hex-style (cond
                            selected? style-3
                            traversed? (assoc style-2 :fill "#4A7A8C" :opacity 0.8)
                            :else style-2)]
            [(if selected? component-svg-rounded-hexagon component-svg-hexagon)
             (assoc hex-style
                    :key (str "hex-" c "-" r)
                    :on-mouse-over (fn [ev] (reset! selected [c r]) nil)
                    :on-mouse-out (fn [ev] (reset! selected nil) nil)
                    :on-click (fn [ev] (put! click-channel [x y]) (reset! selected [c r]) nil))
             x y (* size 0.95)])))])))

(defn component-walker-demo-world [style-1 style-2]
  (let [timeline-walk {:time (atom 0) :go (atom false)}
        position (atom [0 0])
        destination (atom [0 0])
        click-chan (chan)
        speed 2
        show-walker? (atom true)
        traversed-hexes (atom #{[0 0]})
        size 50]
    (go-loop []
      (reset! destination (<! click-chan))
      (recur))
    (go-loop []
      (<! (timeout 16))
      (let [[x2 y2] @destination
            [x1 y1] @position
            distance (m.sqrt (+ (m.pow (- x2 x1) 2) (m.pow (- y2 y1) 2)))
            [xn yn] [(/ (- x2 x1) distance) (/ (- y2 y1) distance)]
            walking? (timeline-walk :go)]
        (if (> distance speed)
          (do
            (if (not @walking?)
              (timeline js/Infinity :atoms timeline-walk))
            (swap! position (fn [[xo yo]] [(+ xo (* xn speed)) (+ yo (* yn speed))]))
            ;; Track traversed hexes
            (let [current-hex (pos-to-hex size x1 y1)]
              (swap! traversed-hexes conj current-hex)))
          (if @walking?
            (do
              (reset! (timeline-walk :go) false)
              (reset! (timeline-walk :time) 0)
              ;; Mark destination hex as traversed
              (let [dest-hex (pos-to-hex size x2 y2)]
                (swap! traversed-hexes conj dest-hex))))))
      (recur))
    (fn []
      [:g
       [component-walker-hex-map style-1 style-2 (assoc style-2 :fill "url(#hatch)") click-chan traversed-hexes]

       ;; Toggle button
       [:g {:transform "translate(-300, -250)"}
        [:rect {:x 0 :y 0 :width 160 :height 30 :rx 5
                :fill (if @show-walker? "#4A7A8C" "#2A4A5C")
                :stroke (:stroke style-1)
                :stroke-width 2
                :style {:cursor "pointer"}
                :on-click #(swap! show-walker? not)}]
        [:text {:x 80 :y 20
                :text-anchor "middle"
                :fill "#DDD"
                :font-family "monospace"
                :font-size "14px"
                :style {:pointer-events "none"}}
         (if @show-walker? "Hide Walker" "Show Walker")]]

       ;; Walker (conditionally shown)
       (when @show-walker?
         [component-walker style-1 style-2 position (timeline-walk :time)])])))
