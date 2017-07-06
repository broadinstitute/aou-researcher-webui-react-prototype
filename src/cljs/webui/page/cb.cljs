(ns webui.page.cb
  (:require
   [dmohs.react :as r]
   [webui.utils :as u]
   ))

(defn- xhr-force-send-with-credentials [data]
  (this-as
   this
   (aset this "withCredentials" true)
   (js-invoke this "_originalSend" data)))

(r/defc CohortBuilder
  {:render
   (fn []
     [:div {:id "ROOT-2521314" :class-name "v-app valo applicationui"
            :style {:height "90vh"}}
      [:div {:class-name "v-app-loading"}]])
   :component-did-mount
   (fn [{:keys [this]}]
     (aset js/XMLHttpRequest "prototype" "_originalSend"
           (aget js/XMLHttpRequest "prototype" "send"))
     (aset js/XMLHttpRequest "prototype" "send" xhr-force-send-with-credentials)
     (u/load-script
      "https://35.185.116.214/pmi-cb/VAADIN/vaadinBootstrap.js?v=7.7.5"
      #(this :-vaadin-bootstrap-loaded)))
   :component-will-unmount
   (fn []
     (js-invoke (aget js/window "location") "reload"))
   ;; All of this represents an effort to clean-up after Vaadin, but it still leaves garbage.
   ;; For now, refreshing the page may be the only option.
   :-unload-vaadin
   (fn [{:keys [this]}]
     (aset js/XMLHttpRequest "prototype" "send"
           (aget js/XMLHttpRequest "prototype" "_originalSend"))
     (->
      (js-invoke js/document "getElementById"
                 "org.vumc.ori.cohortbuilder.widgetsets.CohortBuilderWidgetSet")
      (js-invoke "remove"))
     (-> (js-invoke js/document "getElementById" "ROOT-2521314-overlays")
         (js-invoke "remove"))
     (u/unload-script "https://35.185.116.214/pmi-cb/VAADIN/vaadinBootstrap.js?v=7.7.5")
     (js-delete js/window "vaadin"))
   :-vaadin-bootstrap-loaded
   (fn []
     (when-not (fn? (aget js/window "__gwtStatsEvent"))
       (aset js/vaadin "gwtStatsEvents" #js[])
       (aset js/window "__gwtStatsEvent"
             (fn [e]
               (js-invoke (aget js/vaadin "gwtStatsEvents") "push" e)
               true)))
     (js-invoke
      js/vaadin "initApplication"
      "ROOT-2521314"
      #js{
          "theme" "cohortbuilder"
          "versionInfo" {
                           "vaadinVersion" "7.7.5"
                           },
          "widgetset" "org.vumc.ori.cohortbuilder.widgetsets.CohortBuilderWidgetSet",
          "comErrMsg" {
                         "caption" "Communication problem",
                         "message" "Take note of any unsaved data, and <u>click here</u> or press ESC to continue.",
                         "url" nil
                         },
          "authErrMsg" {
                          "caption" "Authentication problem",
                          "message" "Take note of any unsaved data, and <u>click here</u> or press ESC to continue.",
                          "url" nil
                          },
          "sessExpMsg" {
                          "caption" "Session Expired",
                          "message" "Take note of any unsaved data, and <u>click here</u> or press ESC key to continue.",
                          "url" nil
                          },
          "vaadinDir" "https://35.185.116.214/pmi-cb/VAADIN/",
          "debug" false,
          "standalone" true,
          "heartbeatInterval" 300,
          "serviceUrl" "https://35.185.116.214/pmi-cb/vaadinServlet/",
          "browserDetailsUrl" "https://35.185.116.214/pmi-cb/"
          }))})
