(ns app.main
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [hiccups.runtime :as hiccupsrt])
  (:use [jayq.core :only [$ document-ready html on attr]]))

;;------------------------------------------------------------------------------
;; Atoms
;;------------------------------------------------------------------------------

(def snapshot-list (atom nil))
(def current-index (atom nil))
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

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn on-popstate [])
(defn on-datetime-change [])
(defn on-keydown [])

(defn click-toggle-meridian [])
(defn click-decremen-hour [])
(defn click-increment-hour [])
(defn click-decrement-Minute [])
(defn click-increment-minute [])
(defn click-later [])
(defn click-earlier [])

(defn add-events []
  (aset js/window "onpopstate" on-popstate)
  (on ($ "datepicker") "changeDate" on-datetime-change)
  (on ($ js/document) "keydown" on-keydown)

  (-> ($ "body")
    (on "click" ".toggle-meridian" click-toggle-meridian)
    (on "click" ".decrement-hour" click-decremen-hour)
    (on "click" ".increment-hour" click-increment-hour)
    (on "click" ".decrement-minute" click-decrement-Minute)
    (on "click" ".increment-minute" click-increment-minute)
    (on "click" "#later" click-later)
    (on "click" "#earlier" click-earlier)))

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

(defn init-current-index []
  (let [image (attr ($ "#currentSnapshot") "src")
        starting-index (first (keep-indexed #(if (= image %2) %1) @snapshot-list))]
    (swap! current-index #(if (pos? starting-index)
                                starting-index
                                0))))

(defn init []
  (init-datepicker)
  (init-timepicker) ; seperate namespace?
  (init-snapshot-list)
  (init-current-index) ; current-index change is probably what replaces original update
  (add-events))

(document-ready init)
