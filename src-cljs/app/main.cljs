(ns app.main
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [hiccups.runtime :as hiccupsrt])
  (:use [jayq.core :only [$ document-ready html on attr]]))

;;------------------------------------------------------------------------------
;; Atoms
;;------------------------------------------------------------------------------

(def snapshot-list (atom nil))
(def snapshot-index (atom nil)) ; can this be derived from current time, quickly?
(def selected-time (atom nil))
(def url-update-timout (atom nil)) ; This probably doesn't need to be atom, maybe be clojure feature that handles this

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
  (format "%02d" num))

(defn get-hours []
  (.getHours @selected-time))

(defn get-minutes p[]
  (.getMinutes @selected-time))

(defn set-hours! [new-hours]
  (swap! selected-time doto (.setHours new-hours)))

(defn set-minutes! [new-mins]
  (swap! selected-time doto (.setMinutes new-mins)))

(defn sync-time-with-snapshot! []
  (swap! selected-time #(js/Date. (:time @snapshot-index))))

;;------------------------------------------------------------------------------
;; Watchers
;;------------------------------------------------------------------------------

(defn on-snapshot-index-change [_ _ _ new-index]
  (let [snapshot (@snapshot-list new-index)
        time (Date. (:time snapshot))
        image (:image snapshot)
        time-display (str (.toDateString time) " " (.toLocaleTimeString time))]
    (attr ($ "#currentSnapshot") "src" image)
    (text ($ "#imageDateTime") time-display)
    (prop ($ "#earlier") "disabled" (= (inc new-index) (count @snapshot-list)))
    (prop ($ "#later") "disabled" (zero? new-index))))

(add-watch snapshot-index :_ on-snapshot-index-change)

(defn on-selected-time-change [_ _ _ new-time]
  (let [hour (.getHours new-time)
        meridian (if (> 11 hour) "PM" "AM")
        hour (if (> 12 hour) (- hour 12) hour)]
    (val ($ "#hour") (pad hour))
    (val ($ "#minute") (pad (.getMinutes new-time)))
    (val ($ "#meridian") meridian)
    (.datepicker ($ "#datepicker") "update" new-time)))

(add-watch timestamp :_ on-selected-time-change)

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn on-popstate! [])

(defn on-datetime-change! []
  ); todo

(defn on-keydown! [e]
  (let [key (.-which e)]
    (cond
      (= key 37) (click-earlier!)
      (= key 39) (click-later!))))

(defn click-toggle-meridian! []
  (let [hours (+ (get-hours) 12)
        hours (if (>= 24 ) (- 24 hours) hours)]
    (set-hours! hours)))

(defn click-decrement-hour! []
  (set-hours! (dec (get-hours))))

(defn click-increment-hour! []
  (set-hours! (inc (get-hours))))

(defn click-decrement-Minute! []
  (set-minutes! (dec (get-minutes))))

(defn click-increment-minute! []
  (set-minutes! (inc (get-minutes)))))

(defn click-later! []
  (if (pos? @snapshot-index)
    (do
      (swap! snapshot-index dec)
      (sync-time-with-snapshot!))))

(defn click-earlier! []
  (if (>= @snapshot-index (count @snapshot-list))
    (do
      (swap! snapshot-index inc)
      (sync-time-with-snapshot!))))

(defn add-events []
  (aset js/window "onpopstate" on-popstate!)
  (on ($ "datepicker") "changeDate" on-datetime-change!)
  (on ($ js/document) "keydown" on-keydown!)

  (-> ($ "body")
    (on "click" ".toggle-meridian" click-toggle-meridian!)
    (on "click" ".decrement-hour" click-decremen-hour!)
    (on "click" ".increment-hour" click-increment-hour!)
    (on "click" ".decrement-minute" click-decrement-Minute!)
    (on "click" ".increment-minute" click-increment-minute!)
    (on "click" "#later" click-later!)
    (on "click" "#earlier" click-earlier!)))

;;------------------------------------------------------------------------------
;; App Init
;;------------------------------------------------------------------------------

(defn init-datepicker []
  (let [start-date (js/Date. (:time (last @snapshot-list)))
        end-date (js/Date. (:time (first @snapshot-list)))]
    (.datepicker ($ "#datePicker")
      (js-obj
        "startDate" start-date
        "endDate" end-date))))

(defn init-timepicker []
  )

(defn init-snapshot-list []
  (swap! snapshot-list (fn []
    (-> (.-SNAPSHOT_LIST APP) ; server injected to this in a <script>
      (js->clj)
      (clojure.walk/keywordize-keys)))))

(defn init-snapshot-index []
  (let [image (attr ($ "#currentSnapshot") "src")
        starting-index (first (keep-indexed #(if (= image (:image %2)) %1) @snapshot-list))]
    (swap! snapshot-index #(if (pos? starting-index)
                                starting-index
                                0))))

(defn init []
  (init-datepicker)
  (init-timepicker) ; seperate namespace?
  (init-snapshot-list)
  (init-snapshot-index) ; current-index change is probably what replaces original update
  (add-events))

(document-ready init)
