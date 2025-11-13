(ns motion.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.dom.client :as rdomc]
            [cljs.core.async :refer [<! put! close! timeout chan] :as async]
            [clojure.string :as string]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [motion.utils :refer [svg-arc timeline g-trans make-link]]
            [motion.demos :as demos])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def m js/Math)

(defn handle-svg-click [event-chan [ow oh] ev]
  (when (= (-> ev .-target .-tagName .toLowerCase)
           "svg")
    (put! event-chan ["click" [(- (.-clientX ev) ow) (- (.-clientY ev) oh)]])))

;; -------------------------
;; Components

(defn component-svg-top [ow]
  (let [style {:fill "none" :stroke "#555" :stroke-width "2px" :stroke-linecap "round"}]
    [:g
     [:path (merge style {:d (js/roundPathCorners (str "M 0 45 L 48 45 L 98 5 L " ow " 5") 5 false)})]
     [:path (merge style {:d (js/roundPathCorners (str "M 0 50 L 50 50 L 100 10 L " ow " 10") 5 false)})]]))

(defn component-svg-main [size demo demo-name]
  (let [event-chan (chan)]
    (fn []
      (let [[ow oh] (map #(int (/ % 2)) @size)]
        [:div
         [:svg {:x 0 :y 0 :width "100%" :height "100%" :style {:top "0px" :left "0px" :position "absolute"} :on-click (partial handle-svg-click event-chan [ow oh])}

          (component-svg-top (* ow 2))

          [:g (g-trans ow oh)
           [demo size event-chan]]]

         [:div#back-link
          [:a {:href (make-link "/")} "<-"]]]))))

;; -------------------------
;; Views

(defn component-page-contents []
  [:div#contents
   [:p#contact-link [:a {:href "mailto:chris@mccormick.cx"} "contact"]]
   [:h2 "demos"]
   [:ol
    (doall (map (fn [[d f]]
                  [:li {:key d} [:a {:href (str "v?" d)} d]])
                (partition 2 demos/demos)))]])

(defn component-page-viewer []
  (let [demo-name (js/unescape (get (string/split js/document.location.href "?") 1))
        size (atom [(.-innerWidth js/window) (.-innerHeight js/window)])
        demo (get (apply hash-map demos/demos) demo-name)]
    (fn []
      (if demo
        [component-svg-main size demo demo-name]
        [:div#contents "Demo not found."]))))

;; -------------------------
;; State

(defonce current-page (atom nil))

;; -------------------------
;; Routes

(def routes
  [["/"
    {:name ::home
     :view #'component-page-contents}]
   ["/v"
    {:name ::viewer
     :view #'component-page-viewer}]])

(defn on-navigate [new-match]
  (when new-match
    (reset! current-page (:view (:data new-match)))))

;; -------------------------
;; Views

(defn current-page-component []
  [:div [@current-page]])

;; -------------------------
;; Initialize app

(defonce root
  (rdomc/create-root (.getElementById js/document "app")))

(defn mount-root []
  (rdomc/render root [current-page-component]))

(defn init! []
  (rfe/start!
   (rf/router routes)
   on-navigate
   {:use-fragment false})
  (mount-root))

