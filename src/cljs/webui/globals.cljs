(ns webui.globals
  (:require
   [webui.utils :as u]
   ))

(defonce config nil)

(defn get-auth-instance []
  (js-invoke (aget js/gapi "auth2") "getAuthInstance"))

(defn get-current-user []
  (-> (get-auth-instance) (aget "currentUser") (js-invoke "get")))

(defn get-access-token []
  (-> (get-current-user) (js-invoke "getAuthResponse") (aget "access_token")))

(defn get-profile []
  (-> (get-current-user) (js-invoke "getBasicProfile")))
