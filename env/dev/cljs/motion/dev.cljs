(ns motion.dev
  (:require [motion.core :as core]))

(enable-console-print!)

(defn ^:dev/after-load reload []
  (core/mount-root))

(defn init []
  (core/init!))

