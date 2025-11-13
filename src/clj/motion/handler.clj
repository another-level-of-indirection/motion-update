(ns motion.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup2.core :as h]
            [motion.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]))

(def mount-target
  [:div#app
   [:div {:class "infinitelives-spinner infinitelives-spinner-vertical-center"}]
   [:div {:id "overlay"}]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=0.75, maximum-scale=1.0, user-scalable=no"}]
   [:link {:rel "stylesheet" :href "/css/spinner.css"}]
   [:link {:rel "stylesheet" :href "/css/site.css"}]])

(def loading-page
  (str (h/html
        {:mode :html}
        (h/raw "<!DOCTYPE html>")
        [:html
         (head)
         [:body {:class "body-container"}
          mount-target
          [:script {:src "js/rounding.js"}]
          [:script {:src "js/app.js"}]]])))

(defn index-html []
  "output the HTML as a string"
  (print loading-page))

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/v" [] loading-page)

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
