(ns routing.server
  (:use [routing.examples :only [app]]
        [ring.adapter.undertow :only [run-undertow]]))

(defn run!
  ([] (run! 8080))
  ([port]
   (run-undertow app {:port port})))
