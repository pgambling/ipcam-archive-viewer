(ns app.main
  (:use [jayq.core :only [$ document-ready html on]]))

;;------------------------------------------------------------------------------
;; Atoms
;;------------------------------------------------------------------------------

(def snapshot-list (atom nil)) ; Set this with embedded script or get it at page load?
(def current-indes (atom nil))
(def url-update-timout (atom nil)) ; This probably doesn't need to be atom, maybe be clojure feature that handles this

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
    (on "click" ".toggleMeridian" click-toggle-meridian)
    (on "click" ".decrementHour" click-decremen-hour)
    (on "click" ".incrementHour" click-increment-hour)
    (on "click" ".decrementMinute" click-decrement-Minute)
    (on "click" ".incrementMinute" click-increment-minute)
    (on "click" "#later" click-later)
    (on "click" "#earlier" click-earlier)))

;;------------------------------------------------------------------------------
;; App Init
;;------------------------------------------------------------------------------

(defn init-datepicker [])
(defn init-timepicker [])

(defn init []
  (init-datepicker)
  (init-timepicker) ; seperate namespace?
  ; current-index change is probably what replaces original update
  ; (init-current-index) ; this is probably just swap! identity on current-index atom
  (add-events))

(document-ready init)
