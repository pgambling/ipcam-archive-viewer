(defproject ipcam-archiver "0.0.1"
  :description "IP camera snapshot archiver and viewer"
  :source-paths ["src-clj"]

  :dependencies [
    [org.clojure/clojure "1.5.1"]
    [org.clojure/clojurescript "0.0-2127"
      :exclusions [org.apache.ant/ant]]
    [jayq "2.5.0"]
    [hiccups "0.3.0"]]

  :plugins [
    [lein-cljsbuild "1.0.2"]]

  :cljsbuild {
    :builds [{
      :source-paths ["src-cljs"]
      :compiler {
        :output-dir "public/js/out"
        :output-to "public/js/out/app.js"
        :optimizations :whitespace
        :pretty-print true
        :source-map "public/js/out/app.js.map"
        :externs ["externs/jquery-1.9.js"]}}]})