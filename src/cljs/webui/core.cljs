(ns webui.core
  (:require
   [dmohs.react :as r]
   ))

(r/defc App
  {:render
   (fn [_]
     [:div {}
      [:div {} "Hello World."]
      [:div {:ref "cohort-builder-container" :style {:margin-top "3rem"}}]])
   :component-did-mount
   (fn [{:keys [refs]}]
     (let [script (js-invoke js/document "createElement" "script")]
       (js-invoke script "setAttribute" "src" "VbCohortBuilder.js")
       (js-invoke (aget js/document "body") "appendChild" script)
       (js/setTimeout
        #(js-invoke (aget js/window "VbCohortBuilder")
                    "render" (@refs "cohort-builder-container"))
        100)))})

(defn render-application []
  (r/render
   (r/create-element App)
   (.. js/document (getElementById "app"))))

(render-application)
