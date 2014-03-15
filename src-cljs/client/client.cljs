(ns client
  (:use [jayq.core :only
    [$ document-ready html on attr prop text val]])
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [hiccups.runtime :as hiccupsrt]
    [clojure.walk]
    [cljs.reader]
    [goog.string]
    [goog.string.format]))

(enable-console-print!)

;;------------------------------------------------------------------------------
;; Atoms
;;------------------------------------------------------------------------------

(def snapshot-list (atom nil))
(def snapshot-index (atom nil))
(def selected-time (atom nil))
(def url-update-timeout (atom nil))

;;------------------------------------------------------------------------------
;; Timepicker
;;------------------------------------------------------------------------------

(hiccups/defhtml build-chevron-btn [class-name chevron-class]
  [:a {:class class-name :href "#"}
    [:em {:class chevron-class}]])

(hiccups/defhtml build-increment-btn [class-name]
  (build-chevron-btn class-name "icon-chevron-up"))

(hiccups/defhtml build-decrement-btn [class-name]
  (build-chevron-btn class-name "icon-chevron-down"))

(hiccups/defhtml build-timepicker-input [id]
  [:input {:id id :maxlength "2" :type "text"}])

(hiccups/defhtml build-nbsp-cell []
  [:td.separator "&nbsp;"])

(hiccups/defhtml build-timepicker []
  [:div.timepicker-widget
    [:table
      [:tr
        [:td (build-increment-btn "increment-hour")]
        (build-nbsp-cell)
        [:td (build-increment-btn "increment-minute")]
        (build-nbsp-cell)
        [:td.meridian-column (build-increment-btn "toggle-meridian")]
      [:tr
        [:td (build-timepicker-input "hour")]
        [:td.separator ":"]
        [:td (build-timepicker-input "minute")]
        (build-nbsp-cell)
        [:td (build-timepicker-input "meridian")]]
      [:tr
        [:td (build-decrement-btn "decrement-hour")]
        [:td.separator]
        [:td (build-decrement-btn "decrement-minute")]
        (build-nbsp-cell)
        [:td (build-decrement-btn "toggle-meridian")]]]]])

(defn pad [num]
  "zero pad timepicker digits"
  (goog.string/format "%02d" num))

(defn minutes-to-ms [mins]
  (* mins 60000))

(defn hours-to-ms [hours]
  (* hours 3600000))

(defn sync-time-with-snapshot! []
  (reset! selected-time (:time (nth @snapshot-list @snapshot-index))))

; TODO: This could be improved. Probably need to rethink my data model
(defn show-closest-snapshot! []
  (let [timestamp @selected-time
        earlier-index (first (keep-indexed #(if (>= timestamp (:time %2)) %1) @snapshot-list))
        later-index (max (dec earlier-index) 0)
        later (:time (nth @snapshot-list later-index))
        earlier (:time (nth @snapshot-list earlier-index))
        display-index (if (< (- later timestamp) (- timestamp earlier))
                        later-index
                        earlier-index)]
    (reset! snapshot-index display-index)))

(defn set-url! [timestamp]
  (let [base-url (first (.split js/document.URL "?"))
        new-url (str base-url "?time=" timestamp)]
    (.replaceState js/history nil nil new-url)))

;;------------------------------------------------------------------------------
;; Watchers
;;------------------------------------------------------------------------------

(defn on-snapshot-list-change [_ _ _ snapshot-list]
  (let [start-date (js/Date. (:time (last snapshot-list)))
        end-date (js/Date. (:time (first snapshot-list)))
        datepicker-el ($ "#datepicker")]
    (.datepicker datepicker-el "remove")
    (.datepicker datepicker-el
      (js-obj
        "startDate" start-date
        "endDate" end-date))))

(add-watch snapshot-list :_ on-snapshot-list-change)

(defn on-selected-time-change [_ _ _ new-time]
  (let [datetime (js/Date. new-time)
        hour (.getHours datetime)
        meridian (if (>= hour 12) "PM" "AM")
        hour (if (> hour 12) (- hour 12) hour)
        hour (if (and (= meridian "AM") (= hour 0)) 12 hour)]
    (val ($ "#hour") (pad hour))
    (val ($ "#minute") (pad (.getMinutes datetime)))
    (val ($ "#meridian") meridian)
    (.datepicker ($ "#datepicker") "update" datetime)))

(add-watch selected-time :_ on-selected-time-change)

(defn on-snapshot-index-change [_ _ _ new-index]
  (let [snapshot (nth @snapshot-list new-index)
        timestamp (:time snapshot)
        datetime (js/Date. timestamp)
        image (:image snapshot)
        time-display (str (.toDateString datetime) " " (.toLocaleTimeString datetime))]
    (attr ($ "#currentSnapshot") "src" image)
    (text ($ "#imageDateTime") time-display)
    (prop ($ "#earlier") "disabled" (= (inc new-index) (count @snapshot-list)))
    (prop ($ "#later") "disabled" (zero? new-index))
    ; delay updating the url in case user is rapidly switching times
    (js/clearTimeout @url-update-timeout)
    (reset! url-update-timeout (js/setTimeout (fn [] (set-url! timestamp)) 200))))

(add-watch snapshot-index :_ on-snapshot-index-change)

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn on-popstate [e]
  (if (.-state e)
    (reset! selected-time (:time e))))

(defn on-datetime-change []
  (let [selected-date (.datepicker ($ "#datepicker") "getDate")
        datetime (js/Date. @selected-time)
        new-time (doto selected-date
                    (.setMinutes (.getMinutes datetime))
                    (.setHours (.getHours datetime)))]
    (reset! selected-time (.getTime new-time))
    (show-closest-snapshot!)))

(defn click-toggle-meridian []
  (let [meridian (val ($ "#meridian"))
        hours (if (= meridian "AM") 12 -12)]
    (swap! selected-time + (hours-to-ms hours))
    (show-closest-snapshot!)))

(defn click-decrement-hour []
  (swap! selected-time - (hours-to-ms 1))
  (show-closest-snapshot!))

(defn click-increment-hour []
  (swap! selected-time + (hours-to-ms 1))
  (show-closest-snapshot!))

(defn click-decrement-Minute []
  (swap! selected-time - (minutes-to-ms 1))
  (show-closest-snapshot!))

(defn click-increment-minute []
  (swap! selected-time + (minutes-to-ms 1))
  (show-closest-snapshot!))

(defn change-snapshot-index [change-fn]
  (let [new-index (change-fn @snapshot-index)]
    ; the index can not be outside the bounds of the snapshot list
    (if (<= 0 new-index (dec (count @snapshot-list)))
      (do
        (reset! snapshot-index new-index)
        (sync-time-with-snapshot!)))))

(def click-later (partial change-snapshot-index dec))
(def click-earlier (partial change-snapshot-index inc))

(defn on-keydown [e]
  (let [key (.-which e)]
    (cond
      (= key 37) (click-earlier)
      (= key 39) (click-later))))

(defn add-events []
  (aset js/window "onpopstate" on-popstate)
  (on ($ "#datepicker") "changeDate" on-datetime-change)
  (on ($ js/document) "keydown" on-keydown)

  (-> ($ "body")
    (on "click" ".timepicker-widget a" (fn [e] (.preventDefault e)))
    (on "click" ".toggle-meridian" click-toggle-meridian)
    (on "click" ".decrement-hour" click-decrement-hour)
    (on "click" ".increment-hour" click-increment-hour)
    (on "click" ".decrement-minute" click-decrement-Minute)
    (on "click" ".increment-minute" click-increment-minute)
    (on "click" "#later" click-later)
    (on "click" "#earlier" click-earlier)))

;;------------------------------------------------------------------------------
;; App Init
;;------------------------------------------------------------------------------

(defn init-snapshot-list []
  (->> (text ($ "#snapshot-list")) ; server injected to this in a <script>
       (cljs.reader/read-string)
       (reset! snapshot-list)))

(defn init-snapshot-index []
  (let [image (attr ($ "#currentSnapshot") "src")
        starting-index (first (keep-indexed #(if (= image (:image %2)) %1) @snapshot-list))]
    (reset! snapshot-index
      (if (pos? starting-index)
        starting-index
        0))))

(defn init []
  (html ($ "#timepickerContainer") (build-timepicker))
  (init-snapshot-list)
  (init-snapshot-index)
  (sync-time-with-snapshot!)
  (add-events))

(document-ready init)
