(ns webui.core
  (:require
   [dmohs.react :as r]
   ))

(r/defc App
  {:render
   (fn [_]
     [:div {:style {:padding "1rem 2rem"}}
      [:div {:style {:display "flex" :align-items "flex-end"}}
       [:img {:src "images/all-of-us-logo.svg" :alt "All of Us Logo"
              :style {:width "18rem"}}]
       [:img {:src "images/portal-logo.svg" :alt "Researcher Portal Logo"
              :style {:margin-left "0.5rem" :width "13rem"}}]]
      [:div {:ref "cohort-builder-container" :style {:margin-top "3rem"}}]])
   :component-did-mount
   (fn [{:keys [this]}]
     (let [script (js-invoke js/document "createElement" "script")]
       (js-invoke script "setAttribute" "src" "js/VbCohortBuilder.js")
       (aset script "onload" #(this :-handle-cohort-builder-loaded))
       (js-invoke (aget js/document "body") "appendChild" script)))
   :-handle-cohort-builder-loaded
   (fn [{:keys [refs]}]
     (js-invoke (aget js/window "VbCohortBuilder") "render" (@refs "cohort-builder-container")))})

(defn render-application []
  (r/render
   (r/create-element App)
   (.. js/document (getElementById "app"))))

(render-application)
