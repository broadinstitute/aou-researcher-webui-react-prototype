(ns webui.core
  (:require
   [dmohs.react :as r]
   [webui.auth :as auth]
   [webui.config :as config]
   [webui.globals :as globals]
   [webui.page.cb :as cb]
   [webui.style :as style]
   ))

(r/defc App
  {:render
   (fn [{:keys [this state]}]
     (let [{:keys [auth-loaded? config-loaded? signed-in?]} @state]
       [:div {:style {:padding "1rem 2rem"}}
        [:div {:style {:display "flex" :align-items "flex-end"}}
         [:img {:src "images/all-of-us-logo.svg" :alt "All of Us Logo"
                :style {:width "18rem"}}]
         [:img {:src "images/portal-logo.svg" :alt "Researcher Portal Logo"
                :style {:margin-left "0.5rem" :width "13rem"}}]]
        (cond
          (not config-loaded?)
          [config/ConfigLoader {:on-loaded #(this :-handle-config-loaded)}]
          (not auth-loaded?)
          [auth/GoogleAuthLibLoader {:on-loaded #(this :-handle-auth2-loaded)}]
          :else
          [auth/SignInButton {:hidden? signed-in?}])
        (when signed-in?
          [:div {:style {:margin-top "2rem"}}
           (js-invoke (globals/get-profile) "getName")
           " "
           "(" (js-invoke (globals/get-profile) "getEmail") ")"
           [:div {:style {:margin-top "0.5rem"}}
            [:button {:style style/button
                      :on-click #(js-invoke (globals/get-auth-instance) "signOut")}
             "Sign Out"]]])
        [:div {:style {:margin-top "3rem"}}
         [:button {:on-click #(swap! state update :show-cohort-builder? not)}
          "Toggle Cohort Builder"]
         (when (:show-cohort-builder? @state)
           [:div {:style {:margin-top "1rem"}}
            [cb/CohortBuilder]])]]))
   :-handle-config-loaded
   (fn [{:keys [state]}]
     (swap! state assoc :config-loaded? true))
   :-handle-auth2-loaded
   (fn [{:keys [state]}]
     (-> (globals/get-auth-instance)
         (aget "currentUser")
         (js-invoke "listen" #(swap! state assoc :signed-in? (js-invoke % "isSignedIn"))))
     (swap! state assoc
            :auth-loaded? true
            :signed-in? (-> (globals/get-current-user) (js-invoke "isSignedIn"))))
   :-handle-cohort-builder-loaded
   (fn [{:keys [refs]}]
     (js-invoke (aget js/window "VbCohortBuilder") "render" (@refs "cohort-builder-container")))})

(defn render-application []
  (r/render
   (r/create-element App)
   (.. js/document (getElementById "app"))))

(render-application)
