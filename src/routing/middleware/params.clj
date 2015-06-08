(ns routing.middleware.params
  (:use [ring.middleware.params :only [params-request]]
        [ring.middleware.keyword-params :only [keyword-params-request]]
        [routing.middleware :only [apply-req-mw]]
        [annotate.types :only [Option]]
        routing.types
        [annotate.fns :only [defn$]]))

(defn$ decode ([=> StateFn] [(Option String) => StateFn])
  "Decode params and put in the request map."
  ([] (decode nil))
  ([encoding]
     (fn [state]
       [nil (update-in state [:routing/request] params-request {:encoding encoding})])))

(def ^{:doc "Keywordize param keys."} keywordize
  (apply-req-mw keyword-params-request))
