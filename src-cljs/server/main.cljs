(ns server.main
  (:require
    [cljs.nodejs :as nodejs]
    [cljs.reader]
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

(defn log-error [msg]
  (.error js/console msg))

(defn read-config []
  (->> (.readFileSync fs "./config.cljs")
       (.toString)
       (cljs.reader/read-string)
       (reset! config))
  (let [file (.toString (.readFileSync fs "./config.cljs"))
        file (.replace file (js/RegExp. "\\r|\\n" "g") "")
        file (str "{:foo 1}")]
    (.log js/console file)
    (.log js/console (pr-str (cljs.reader/read-string "{:foo 1}")))))

(defn generate-file-name [time]
  (-> time
    (js/Date.)
    (.toISOString)
    (.replace (js/RegExp. ":" "g"))
    (.substr 0 17)
    (str "Z.jpg")))

(defn filename-to-time [fname]
  (let [date-str (first (clojure.sftring/split fname "."))
        hour (subs date-str 0 13)
        min (subs date-str 13 15)
        seconds (subs date-str 15)]
    (js/Date. (str hour ":" min ":" seconds))))

(defn on-get-snapshot [response]
  (let [file-path (.join path (:archive-dir @config) (generate-file-name (js/Date.)))
        file (.createWriteStream fs file-path)]
    (.pipe response file)
    (.on file "finish" #(.close file))))

(defn on-get-snapshot-error [e]
  (log-error (str "Error downloading snapshot: " (.-message e))))

(defn download-snapshot []
  (-> http
      (.get (:camera-url @config) on-get-snapshot)
      (.on "error" on-get-snapshot-error)))

(defn generate-snapshot-list []
  (.readdir fs (:archive-dir @config) (fn [err files]
    (if (err) (throw (js/Error. "Error reading snapshot directory!")))
    (-> files
      (filter #(= (.substr (- (count %) 3) %) "jpg"))
      (map #({:image % :time (filename-to-time %)}))
      (sort-by :time)
      (reset! @snapshot-list)))))

(defn delete-image [fname]
  (.unlink fs (.join path (:archive-dir @config) fname)))

(defn delete-old-images []
  (let [now (.getTime (js/Date.))
        days-to-archive-ms (:num-days-archive @config)
        earliest-time-to-keep (- now (days-to-ms days-to-archive-ms))
        older-than-earliest? (fn [file] (< (:time file) earliest-time-to-keep))]
    (->> @snapshot-list
      (filter older-than-earliest?)
      (map :image)
      (map delete-image)
      doall))
  (generate-snapshot-list))

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
    (let [snapshot (first (filter #(= (:time %) time) @snapshot-list))]
      (if (nil? snapshot)
        (.send res 404)
        (.sent res (index (:time snapshot)))))))

(defn not-found [req res]
  (.send res 404))

;;------------------------------------------------------------------------------
;; server initialization
;;------------------------------------------------------------------------------

(defn -main [& args]
  (when-not (.existsSync fs "./config.cljs")
    (log-error "ERROR: config.cljs not found")
    (log-error "HINT: Use example_config.cljs to start a new one.")
    (.exit js/process 1))

  (reset! config (read-config))

  ; start polling camera
  (download-snapshot)
  (js/setInterval download-snapshot (* (:poll-interval @config) 1000))

  ; delete old images once an hour
  (delete-old-images)
  (js/setInterval delete-old-images (* 60 60 1000))

  ; configure and start the server
  (let [app (express)
        static-file-handler (aget express "static")
        one-year (* 365 24 60 60 1000)
        static-opts (js-obj "maxAge" one-year)
        port (:port @config)]
    (doto app
      (.use (.compress express))
      (.get "/" index-handler)
      (.use (static-file-handler (str js/__dirname "/public") static-opts))
      (.use (static-file-handler (:archive-dir @config) static-opts))
      (.use not-found)
      (.listen port)
      (println "Listening on port " port))))

(set! *main-cli-fn* -main)
