(ns webui.config
  (:require
   [dmohs.react :as r]
   [promesa.core :as p]
   [webui.globals :as globals]
   [webui.utils :as u]
   ))

(r/defc ConfigLoader
  {:render
   (fn [{:keys [state]}]
     [:div {:style {:margin "1rem 0"}}
      (if-let [error (:error @state)]
        error
        "Loading config...")])
   :component-did-mount
   (fn [{:keys [this props state]}]
     (->> (u/ajax {:url "/config.json"})
          (p/map (fn [{:keys [success? response-text]}]
                   (if success?
                     (let [parsed (u/parse-json-string response-text)]
                       (if (u/error? parsed)
                         (swap! state assoc :error "Error parsing configuration.")
                         (do
                           (set! globals/config parsed)
                           (u/call (:on-loaded props)))))
                     (swap! state assoc :error response-text))))))})
