(ns server
  (:require [cljs.nodejs :as nodejs]))

(defn -main [& args]
  (println (apply str (map [\ "world" "hello"] [2 0 1]))))

(nodejs/enable-util-print!)

(set! *main-cli-fn* -main)
