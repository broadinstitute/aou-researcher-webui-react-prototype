(ns webui.utils
  (:require-macros
   [webui.utils :refer [log jslog cljslog pause restructure]])
  (:require
    cljs.pprint
    [clojure.string :refer [join lower-case split]]
    [promesa.core :as p]
    ))

(defn call [f & args]
  (apply f args))

(defn error? [x]
  (instance? js/Error x))

(defn error->nil [x]
  (when-not (error? x) x))

(defn error [message data]
  (let [e (js/Error. message)]
    (aset e "data" data)
    e))

(defn error-data [e] (aget e "data"))

(defn transform-keys [keyfn xs]
  "Transforms keyword keys with the given function. String keys are left alone."
  ;; Structure taken from js->clj source.
  (let [f (fn thisfn [x]
            (cond
              (map? x)
              (reduce-kv (fn [r k v]
                           (assoc r (if (keyword? k) (keyfn k) k) (thisfn v))) {} x)
              (seq? x)
              (doall (map thisfn x))
              (coll? x)
              (into (empty x) (map thisfn x))
              :else x))]
    (f xs)))

(defn ->snake-case-keys [xs]
  (transform-keys
   (fn [k]
     (-> k
         name
         (clojure.string/replace "-" "_")
         keyword))
   xs))

(defn ->camel-case-keys [xs]
  (transform-keys
   (fn [k]
     (let [words (clojure.string/split (name k) #"-")
           cased (map #(str (clojure.string/upper-case (subs % 0 1)) (subs % 1)) (rest words))]
       (keyword (apply str (first words) cased))))
   xs))

(defn parse-json-string [s]
  (try
    (js->clj (js/JSON.parse s) :keywordize-keys true)
    (catch js/Error e
      e)))

(def content-type=json {"Content-Type" "application/json"})

(defn- create-ajax-response-map [xhr]
  (let [status-code (.-status xhr)]
    {:xhr xhr
     :status-code status-code
     :success? (<= 200 status-code 299)
     :status-text (.-statusText xhr)
     :response-text (.-responseText xhr)}))

(defn ajax [{:keys [url method headers data with-credentials?]}]
  (assert url (str "Missing url parameter"))
  (p/promise
   (fn [resolve reject]
     (let [method (if method (clojure.string/upper-case (name method)) "GET")
           xhr (js/XMLHttpRequest.)]
       (when with-credentials?
         (set! (.-withCredentials xhr) true))
       (.addEventListener xhr "loadend" #(resolve (create-ajax-response-map xhr)))
       (.open xhr method url)
       (doseq [[k v] headers]
         (.setRequestHeader xhr k v))
       (if data
         (.send xhr data)
         (.send xhr))))))

(defn get-exponential-backoff-interval [attempt]
  (* (.pow js/Math 2 attempt) 1000)) ;; backoff interval in millis

(defn deep-merge [& maps]
  (doseq [x maps] (assert (or (nil? x) (map? x)) (str "not a map: " x)))
  (apply
    merge-with
    (fn [x1 x2] (if (and (map? x1) (map? x2)) (deep-merge x1 x2) x2))
    maps))

(defn generate-form-data
  "Create a blob of multipart/form-data from the provided map."
  [params]
  (let [form-data (js/FormData.)]
    (doseq [[k v] params]
      (.append form-data (name k) v))
    form-data))

(defn distance [x1 y1 x2 y2]
  (let [dx (- x1 x2)
        dy (- y1 y2)]
    (js/Math.sqrt (+ (* dx dx) (* dy dy)))))

(defn map-kv [f m]
  (into (empty m)
        (map (fn [[k v]] (f k v)) m)))

(defn maybe-pluralize [number unit]
  (if (> number 1)
    (str number " " unit "s")
    (str number " " unit)))

(defn log-methods [prefix defined-methods]
  (map-kv (fn [method-name method]
            [method-name
             (fn [& args]
               (log (str prefix " - " (name method-name)))
               (apply method args))])
          defined-methods))

(defn with-window-listeners [listeners-map defined-methods]
  (let [did-mount
        (fn [{:keys [locals] :as data}]
          (doseq [[event function] listeners-map]
            (let [func (partial function data)]
              (swap! locals assoc (str "WINDOWLISTENER " event) func)
              (.addEventListener js/window event func)))
          (when-let [defined-did-mount (:component-did-mount defined-methods)]
            (defined-did-mount data)))
        will-unmount
        (fn [{:keys [locals] :as data}]
          (doseq [[event _] listeners-map]
            (.removeEventListener js/window event (@locals (str "WINDOWLISTENER " event))))
          (when-let [defined-will-unmount (:component-will-unmount defined-methods)]
            (defined-will-unmount data)))]
    (assoc defined-methods
      :component-did-mount did-mount
      :component-will-unmount will-unmount)))
