(ns server.html
  (:require-macros [hiccups.core :as hiccups])
  (:require [hiccups.runtime :as hiccupsrt]))

(hiccups/defhtml index [title snapshot-list initial-image]
  [:html
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:content "IE=edge,chrome=1", :http-equiv "X-UA-Compatible"}]
   [:title title]
   [:meta {:content "width=device-width", :name "viewport"}]
   "<!-- HTML5 shim, for IE6-8 support of HTML elements -->"
   "<!--[if lt IE 9]><script src=\"http://html5shim.googlecode.com/svn/trunk/html5.js\"></script><![endif]-->"
   [:link
    {:href "css/bootstrap-2.3.2.min.css",
     :type "text/css",
     :rel "stylesheet"}]
   [:link
    {:href "css/bootstrap-datepicker-20130809.css",
     :type "text/css",
     :rel "stylesheet"}]
   [:link {:href "css/main.css", :type "text/css", :rel "stylesheet"}]]
  [:body
   [:div#bodyContainer
    [:h1#imageDateTime "Loading..."]
    [:div#imageContainer
     [:img#currentSnapshot
      {:alt "camera snapshot", :src initial-image}]
     [:div#controls
      [:button#earlier.btn.btn-large "« Earlier"]
      [:button#later.btn.btn-large "Later »"]]]
    [:div#datetimeInputContainer
     [:div#datepicker]
     [:div#timepickerContainer]]]
   [:script {:src "js/libs/jquery-1.10.1.min.js"}]
   [:script {:src "js/libs/bootstrap-datepicker-20130809.js"}]
   [:script#snapshot-list {:type "application/edn"} (pr-str snapshot-list)]
   [:script {:src "js/out/client.js"}]]])
