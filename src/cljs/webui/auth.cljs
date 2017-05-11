(ns webui.auth
  (:require
   [dmohs.react :as r]
   [webui.globals :as globals]
   [webui.style :as style]
   [webui.utils :as u]
   ))

(r/defc GoogleAuthLibLoader
  {:render
   (fn [{:keys [state]}]
     [:div {:style {:margin "1rem 0"}}
      (if-let [error (:error @state)]
        error
        "Loading auth...")])
   :component-did-mount
   (fn [{:keys [this state]}]
     (js-invoke js/gapi "load" "auth2" #(this :-handle-auth2-loaded)))
   :-handle-auth2-loaded
   (fn [{:keys [props state]}]
     (let [{:keys [on-loaded]} props
           init-options (clj->js {:client_id (:google-client-id globals/config)
                                  :hosted_domain "pmi-ops.org"})]
       (-> (js-invoke (aget js/gapi "auth2") "init" init-options)
           (js-invoke "then" on-loaded #(swap! state assoc :error (aget % "details"))))))})

(r/defc SignInButton
  {:render
   (fn [{:keys [this props state]}]
     (let [{:keys [error-key]} @state]
       ;; Google's code complains if the sign-in button goes missing, so we hide this component
       ;; rather than removing it from the page.
       [:div {:style {:display (when (:hidden? props) "none")
                      :margin-top "2rem"}}
        [:div {:style {:display "flex" :align-items "center"}}
         [:button {:style style/button :on-click #(this :-handle-sign-in-click)}
          "Sign In"]
         [:span {:style {:padding-left "1ex"}}
          "with your " [:strong {} "pmi-ops.org"] " account."]]
        [:div {:style {:margin-top "1.5rem"}}
         [:a {:style {:flex "0 10 5rem"}
              :href "#register"}
          "Register"]
         [:span {}
          " for a " [:strong {} "pmi-ops.org"] " account."]]
        (when error-key
          [:div {}
           (if (= error-key :domain)
             (str "You must sign-in with a pmi-ops.org account. If you do not have a pmi-ops.org"
                  " account, you may create one by clicking the \"Register\" button.")
             "Sign-in failure.")])]))
   :-handle-sign-in-click
   (fn [{:keys [this props]}]
     (let [{:keys [on-change]} props]
       (-> (globals/get-auth-instance)
           (js-invoke "signIn")
           (js-invoke "then" (constantly nil) #(this :-handle-sign-in-error %)))))
   :-handle-sign-in-error
   (fn [{:keys [state]} e]
     (let [account-domain (aget e "accountDomain")
           expected-domain (aget e "expectedDomain")]
       (if (and account-domain expected-domain (not= account-domain expected-domain))
         (swap! state assoc :error-key :domain)
         (swap! state assoc :error-key :unknown))))})
