(ns routing.middleware.logging
  (:use [annotate.fns :only [defn$]]
        [annotate.types :only [Subset Keyword]]
        routing.types)
  (:require [clojure.tools.logging :as log]
            [routing.util :as util]))

(defn$ log* [State Keyword String => State]
  {:private true}
  [state level msg]
  (update-in state [:routing/middleware :logs]
             #(if % (conj % [level msg]) [[level msg]])))

(defmacro deflogfn [level doc-string]
  `(defn$ ~level [String ~'=> StateFn]
     ~doc-string
     [msg#]
     (fn [state#]
       [nil (log* state# ~(keyword level) msg#)])))

(deflogfn fatal "Log at the fatal level")
(deflogfn error "Log at the error level")
(deflogfn warn "Log at the warn level")
(deflogfn info "Log at the info level")
(deflogfn debug "Log at the debug level")
(deflogfn trace "Log at the trace level")

(def log-levels #{:fatal :error :warn :info :debug :trace})
(def LogLevels (Subset log-levels))

(defn$ log! ([Response => Response] [Response LogLevels => Response])
  "Log messages at all levels or only a subset."
  ([resp] (log! resp log-levels))
  ([resp levels]
   (if (or (contains? (:routing/middleware resp) :logs)
           (contains? resp :logs))
     (do
       (let [logs (concat (get-in resp [:routing/middleware :logs])
                          (:logs resp))]
         ;; Log messages
         (doseq [[level msg] logs
                 :when (levels level)]
           (log/log level msg)))
       (-> resp
           (dissoc :logs)
           (util/dissoc-in [:routing/middleware :logs])))
     resp)))
