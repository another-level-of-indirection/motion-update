(ns motion.demo-agents
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [<! timeout] :as async]
            [clojure.string :as string]
            [motion.utils :refer [timeline]]
            [motion.styles :refer [colors styles]]
            [sci.core :as sci])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def m js/Math)

;; Sample expressions for the asker to send
(def sample-expressions
  ["(+ 1 2 3)"
   "(* 5 7)"
   ;;    "(map inc [1 2 3])"
   ;;    "(filter odd? [1 2 3 4 5])"
   ;;    "(reduce + [10 20 30])"
   ;;    "(count \"hello\")"
   ;;    "(reverse [1 2 3])"
   ;;    "(take 3 (range 10))"
   ;;    "(str \"Hello\" \" \" \"World\")"
   ;;    "(Math/sqrt 16)"
   ])

(defn evaluate-expression [expr-str allowed-symbols]
  (try
    (let [result (sci/eval-string expr-str {:allow allowed-symbols})]
      {:success true :result (pr-str result)})
    (catch :default _e
      {:success false :result "Don't know"})))

(defn component-agent [x y label color pulse-t]
  (let [pulse-scale (+ 1 (* 0.1 (m.sin (* pulse-t 0.003))))
        lines (string/split label #"\n")]
    [:g {:transform (str "translate(" x "," y ")")}
     ;; Agent body (hexagon)
     [:g {:transform (str "scale(" pulse-scale ")")}
      [:circle {:cx 0 :cy 0 :r 30
                :fill "none"
                :stroke color
                :stroke-width 2}]
      [:circle {:cx 0 :cy 0 :r 20
                :fill color
                :fill-opacity 0.2
                :stroke color
                :stroke-width 1}]]
     ;; Label
     [:text {:x 0 :y (- 55 (* (dec (count lines)) 7))
             :text-anchor "middle"
             :fill color
             :font-size 12
             :font-family "monospace"}
      (for [[idx line] (map-indexed vector lines)]
        ^{:key idx}
        [:tspan {:x 0 :dy (if (zero? idx) 0 14)} line])]]))

(defn component-message-bubble [x y text color side t]
  (let [bubble-width 180
        bubble-height 40
        fade-in (m.min 1 (/ t 200))
        y-offset (* (- 1 fade-in) 10)]
    [:g {:opacity fade-in
         :transform (str "translate(" x "," (- y y-offset) ")")}
     ;; Bubble background
     [:rect {:x (if (= side :left) -10 (- 10 bubble-width))
             :y -20
             :width bubble-width
             :height bubble-height
             :rx 5
             :ry 5
             :fill "#2a2a2a"
             :stroke color
             :stroke-width 1}]
     ;; Bubble pointer
     ;;  [:path {:d (if (= side :left)
     ;;               "M -10 0 L -25 5 L -10 10"
     ;;               (str "M " (+ 10 (- bubble-width)) " 0 L " (+ 25 (- bubble-width)) " 5 L " (+ 10 (- bubble-width)) " 10"))
     ;;          :fill "#2a2a2a"
     ;;          :stroke color
     ;;          :stroke-width 1}]
     ;; Text
     [:text {:x (if (= side :left) 0 (- 0))
             :y 0
             :text-anchor (if (= side :left) "start" "end")
             :fill color
             :font-size 12
             :font-family "monospace"
             :dominant-baseline "middle"}
      text]]))

(defn component-transmission [x1 y1 x2 y2 progress color]
  (when (and (> progress 0) (< progress 1))
    (let [x (+ x1 (* (- x2 x1) progress))
          y (+ y1 (* (- y2 y1) progress))
          particle-size (* 4 (m.sin (* progress m.PI)))]
      [:g
       ;; Transmission line
       [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2
               :stroke color
               :stroke-width 1
               :stroke-dasharray "5,5"
               :opacity 0.3}]
       ;; Moving particle
       [:circle {:cx x :cy y :r particle-size
                 :fill color
                 :opacity 0.8}]
       [:circle {:cx x :cy y :r (* particle-size 2)
                 :fill "none"
                 :stroke color
                 :stroke-width 1
                 :opacity 0.4}]])))

(defn component-demo-agents [size event-chan]
  (let [tellers [{:id :teller-add
                  :label "TELLER\n(+)"
                  :y -80
                  :allowed '[+]
                  :color (colors :yellow)}
                 {:id :teller-mult
                  :label "TELLER\n(*)"
                  :y 0
                  :allowed '[*]
                  :color (colors :green)}
                 {:id :teller-both
                  :label "TELLER\n(+ *)"
                  :y 80
                  :allowed '[+ *]
                  :color (colors :orange)}]
        state (atom {:phase :idle
                     :current-expr nil
                     :current-results {}
                     :expr-index 0
                     :time 0})
        tl {:time (atom 0) :go (atom true)}]

    ;; Main animation loop
    (go-loop []
      (let [t @(tl :time)
            phase (:phase @state)]
        (cond
          ;; Idle phase - wait for next cycle
          (= phase :idle)
          (when (> t 1000)
            (let [expr-idx (:expr-index @state)
                  expr (get sample-expressions expr-idx)]
              (swap! state assoc
                     :phase :asking
                     :current-expr expr
                     :current-results {})
              (reset! (tl :time) 0)))

          ;; Asking phase - show expression
          (= phase :asking)
          (when (> t 1000)
            (swap! state assoc :phase :transmitting)
            (reset! (tl :time) 0))

          ;; Transmitting phase - show animation
          (= phase :transmitting)
          (when (> t 800)
            (let [expr (:current-expr @state)
                  results (into {} (map (fn [teller]
                                          [(:id teller)
                                           (evaluate-expression expr (:allowed teller))])
                                        tellers))]
              (swap! state assoc
                     :phase :responding
                     :current-results results)
              (reset! (tl :time) 0)))

          ;; Responding phase - show result
          (= phase :responding)
          (when (> t 2000)
            (swap! state assoc :phase :idle)
            (swap! state update :expr-index #(mod (inc %) (count sample-expressions)))
            (reset! (tl :time) 0))))

      (<! (timeout 16))
      (recur))

    ;; Start timeline
    (timeline js/Infinity :atoms tl)

    ;; Render component
    (fn []
      (let [t @(tl :time)
            {:keys [phase current-expr current-results]} @state
            asker-x -120
            teller-x 120]
        [:g (styles :blue-line)
         ;; Asker agent
         [component-agent asker-x 0 "ASKER" (colors :blue) t]

         ;; Teller agents
         (for [teller tellers]
           ^{:key (:id teller)}
           [component-agent teller-x (:y teller) (:label teller) (:color teller) t])

         ;; Communication visualization
         (case phase
           :asking
           [component-message-bubble (- asker-x 45) -35
            current-expr (colors :blue) :left t]

           :transmitting
           [:g
            [component-message-bubble (- asker-x 45) -35
             current-expr (colors :blue) :left 1000]
            (for [teller tellers]
              ^{:key (:id teller)}
              [component-transmission (+ asker-x 30) 0 (- teller-x 30) (:y teller)
               (/ t 800) (colors :orange)])]

           :responding
           [:g
            (for [teller tellers]
              (let [result (get current-results (:id teller))]
                ^{:key (:id teller)}
                [component-message-bubble (+ teller-x 45) (- (:y teller) 35)
                 (:result result)
                 (if (:success result) (:color teller) (colors :red))
                 :right t]))]

           ;; Default (idle)
           nil)

         ;; Status text
         [:text {:x 0 :y 140
                 :text-anchor "middle"
                 :fill (colors :white)
                 :font-size 10
                 :font-family "monospace"
                 :opacity 0.5}
          (str "Phase: " (name phase))]]))))

(defn component-demo-rainbow-orb []
  ;; This is just a placeholder in case it's referenced
  [:g])

