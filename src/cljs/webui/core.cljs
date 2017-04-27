(ns webui.core
  (:require
   [dmohs.react :as r]
   ))

(r/defc App
  {:render
   (fn [_]
     [:div {} "Hello World."])})

(defn render-application []
  (r/render
   (r/create-element App)
   (.. js/document (getElementById "app"))))

(render-application)
