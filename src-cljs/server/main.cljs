(ns server.main
  (:require
    [cljs.nodejs :as nodejs]
    [clojure.walk]
    [clojure.string :as string]
    [server.html :as html]))

;;------------------------------------------------------------------------------
;; node dependencies
;;------------------------------------------------------------------------------

(def fs (js/require "fs"))
(def path (js/require "path"))
(def http (js/require "http"))
(def async (js/require "async"))
(def express (js/require "express"))

;;------------------------------------------------------------------------------
;; atoms
;;------------------------------------------------------------------------------

(def config (atom nil))
(def snapshot-list (atom nil))

;;------------------------------------------------------------------------------
;; general functionality
;;------------------------------------------------------------------------------

(defn days-to-ms [days]
  (* days 86400000))

(def log-error [msg]
  (.error console msg))

(def read-config []
  (-> (js/require "./config.json")
      (js->clj)
      (clojure.walk/keywordize-keys)))

(defn generate-file-name [time]
  (-> time
    (js/Date.)
    (.toISOString)
    (.replace (js/RegExp. ":" "g"))
    (.substr 0 17)
    (str "Z.jpg")))

(defn filename-to-time [fname]
  (let [date-str (first (clojure.string/split fname "."))
        hour (subs date-str 0 13)
        min (subs date-str 13 15)
        seconds (subs date-str 15)]
    (js/Date. (str hour ":" min ":" seconds))))

(defn on-get-snapshot [response]
  (let [file-path (.join path (:archive-dir @config) (generate-file-name (Date.)))
        file (.createWriteStream fs file-path)]
    (.pipe response file)
    (.on file "finish" #(.close file))))

(defn on-get-snapshot-error [e]
  (log-error (str "Error downloading snapshot: " (.-message e))))

(defn download-snapshot [camera-url archive-dir]
  (-> http
      (.get (:camera-url @config) on-get-snapshot)
      (.on "error" on-get-snapshot-error)))

(defn delete-image [fname]
  (.unlink fs (.join path (:archive-dir @config) fname)))

(defn delete-old-images [num-days archive-dir]
  (let [now (.getTime (Date.))
        days-to-archive-ms (:num-days-archive @config)
        earliest-time-to-keep (- now (days-to-ms days-to-archive-ms))
        older-than-earliest? (fn [file] (< (:time file) earliest-time-to-keep))]
    (->> @snapshot-list
      (filter older-than-earliest?)
      (map :image)
      (map delete-image)
      doall))
  (generate-snapshot-list))

(defn generate-snapshot-list []
  (.readdir fs (:archive-dir @config) (fn [err files]
    (if (err) (throw js/Error. "Error reading snapshot directory!"))
    (-> files
      (filter #(= (.substr (- (count %) 3) %) "jpg"))
      (map #({:image % :time (filename-to-time %)}))
      (sort-by :time)
      (reset! @snapshot-list)))))

;;------------------------------------------------------------------------------
;; route handlers
;;------------------------------------------------------------------------------

(defn index-handler [req res]
  (let [index (partial html/index (:title @config) @snapshot-list)
        time (js/parseInt (.-time (.-query req)) 10)]
    ; default to first snapshot
    (if (js/isNaN time)
      (.sent res (index (:time (first @snapshot-list)))))

    ; load page with requested snapshot
    (let [snapshot (first (filter #(= (:time %) time)))]
      (if (nil? snapshot)
        (.send res 404)
        (.sent res (index (:time snapshot)))))))

;;------------------------------------------------------------------------------
;; server initialization
;;------------------------------------------------------------------------------

(defn -main [& args]
  (when-not (.existsSync fs "./config.json")
    (log-error "ERROR: config.json not found")
    (log-error "HINT: Use example_config.json to start a new one.")
    (.exit process 1))

  (reset! config (read-config))

  ; start polling camera
  (download-snapshot)
  (js/setInterval download-snapshot (* (:poll-interval @config) 1000))

  ; delete old images once an hour
  (delete-old-images)
  (js/setInterval delete-old-images (* 60 60 1000))

  ; configure and start the server
  (let [app (express)
        one-year (* 365 24 60 60 1000)
        static-opts (js-obj "maxAge" one-year)
        port (:port @config)]
    (doto app
      (.use (.compress express))
      (.get "/" index-handler)
      (.use (.static express (str js/__dirname "/public") static-opts))
      (.use (.static express (:archive-dir @config) static-opts))
      (.use not-found)
      (.listen port)
      (println "Listening on port " port))))

(nodejs/enable-util-print!)

(set! *main-cli-fn* -main)
