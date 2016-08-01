(ns routing.core
  (:require [clojure.string :as string]
            [routing.response :as resp])
  (:use [routing.parse :only [parsers]]
        [ring.util.response :only [file-response]]
        [monads.state :only [state->]]
        [monads.maybe :only [maybe->]]
        routing.util
        [annotate.fns :only [defn$]]
        [annotate.types :only [Any Seq Regex Fn Int IFn LazySeq Named]]
        routing.types
        [stch.glob :only [match-glob compile-pattern*]]))

(defn$ reduce-forms [State (Seq) => [Any State]]
  [state0 forms]
  (reduce (fn [[v state1] form]
            (if (:routing/matched? state1)
              (reduced [v state1])
              (if (fn? form)
                (form state1)
                [form state1])))
          [resp/empty-resp state0]
          forms))

(defmacro routes
  [& body]
  `(fn [state#] (reduce-forms state# (list ~@body))))

(defmacro defroutes
  "Define a routing function that can be used with
  defrouter."
  [n & body]
  `(def ~n (routes ~@body)))

(defmacro let-routes
  [bindings & body]
  `(fn [state#]
     (let ~bindings (reduce-forms state# (list ~@body)))))

(defmacro state-routes
  "Combine state-> with routes."
  [bindings & body]
  `(state-> ~bindings (routes ~@body)))

(defmacro maybe-routes
  [bindings & body]
  `(fn [state#]
     (maybe-> ~bindings (reduce-forms state# (list ~@body)))))

(defn$ -path [Regex StateFn => StateFn]
  [pattern f]
  (fn [state0]
    (maybe-> [segment (next-path-segment state0)
              _ (match-glob pattern segment)]
      (let [state1 (consume-path-segment state0)
            [v state2] (f state1)]
        (if (:routing/matched? state2)
          [v state2]
          [resp/empty-resp state1]))
      [resp/empty-resp state0])))

(defmacro path
  [segment & body]
  `(-path ~(compile-pattern* segment) (routes ~@body)))

(defn$ -method [RequestMethod StateFn => StateFn]
  [meth f]
  (fn [state0]
    (if (= meth (:routing/request-method state0))
      (let [[v state1] (f state0)]
        (if (:routing/matched? state1)
          [v state1]
          [resp/empty-resp state0]))
      [resp/empty-resp state0])))

(defn- mk-bindings [bindings]
  (if (empty? bindings) [(gensym)]
      bindings))

(defmacro method
  [meth & body]
  `(-method ~meth (routes ~@body)))

(defmacro GET
  [& body]
  `(method :get ~@body))

(defmacro POST
  [& body]
  `(method :post ~@body))

(defmacro PUT
  [& body]
  `(method :put ~@body))

(defmacro DELETE
  [& body]
  `(method :delete ~@body))

(defmacro PATCH
  [& body]
  `(method :patch ~@body))

(defn$ -segment [[Regex Fn] StateFn => StateFn]
  [[pattern parser] f]
  (fn [state0]
    (maybe-> [segment (next-path-segment state0)
              _ (re-matches pattern segment)]
      (let [state1 (consume-path-segment state0)
            parsed-segment (parser segment)
            [v state2] ((f parsed-segment) state1)]
        (if (:routing/matched? state2)
          [v state2]
          [resp/empty-resp state1]))
      [resp/empty-resp state0])))

(defmacro segment
  [pattern-parser bindings & body]
  `(-segment ~pattern-parser (fn ~(mk-bindings bindings) (routes ~@body))))

(defmacro match-int
  [bindings & body]
  `(segment (parsers :int) ~bindings ~@body))

(defmacro match-date
  [bindings & body]
  `(segment (parsers :date) ~bindings ~@body))

(defmacro match-uuid
  [bindings & body]
  `(segment (parsers :uuid) ~bindings ~@body))

(defmacro match-slug
  [bindings & body]
  `(segment (parsers :slug) ~bindings ~@body))

(defn$ -match-regex [Regex StateFn => StateFn]
  [pattern f]
  (fn [state0]
    (maybe-> [segment (next-path-segment state0)
              matches (re-matches pattern segment)]
      (let [state1 (consume-path-segment state0)
            [v state2] ((f matches) state1)]
        (if (:routing/matched? state2)
          [v state2]
          [resp/empty-resp state1]))
      [resp/empty-resp state0])))

(defmacro match-regex
  [pattern bindings & body]
  `(-match-regex ~pattern (fn ~(mk-bindings bindings) (routes ~@body))))

(defn$ -params [[Named] StateFn => StateFn]
  [ps f]
  (fn [state]
    (let [params (-> state :routing/request :params)]
      ((f (select-values params ps)) state))))

(defmacro params
  [ps bindings & body]
  `(-params ~ps (fn ~bindings (routes ~@body))))

(defn$ -param [Named StateFn => StateFn]
  [p f]
  (fn [state]
    (let [params (-> state :routing/request :params)]
      ((f (get params p)) state))))

(defmacro param
  [p bindings & body]
  `(-param ~p (fn ~bindings (routes ~@body))))

(defn$ -scheme [Scheme StateFn => StateFn]
  [s f]
  (fn [state0]
    (if (= s (-> state0 :routing/request :scheme))
      (let [[v state1] (f state0)]
        (if (:routing/matched? state1)
          [v state1]
          [resp/empty-resp state0]))
      [resp/empty-resp state0])))

(defmacro scheme
  [s & body]
  `(-scheme ~s (routes ~@body)))

(defmacro http
  [& body]
  `(scheme :http ~@body))

(defmacro https
  [& body]
  `(scheme :https ~@body))

(defn$ -port [Int StateFn => StateFn]
  [p f]
  (fn [state0]
    (if (= p (-> state0 :routing/request :server-port))
      (let [[v state1] (f state0)]
        (if (:routing/matched? state1)
          [v state1]
          [resp/empty-resp state0]))
      [resp/empty-resp state0])))

(defmacro port
  [p & body]
  `(-port ~p (routes ~@body)))

(defn$ -remote-address [Regex StateFn => StateFn]
  [pattern f]
  (fn [state0]
    (maybe-> [ra (-> state0 :routing/request :remote-addr)
              _ (match-glob pattern ra)]
      (let [[v state1] (f state0)]
        (if (:routing/matched? state1)
          [v state1]
          [resp/empty-resp state0]))
      [resp/empty-resp state0])))

(defmacro remote-address
  [ra & body]
  `(-remote-address ~(compile-pattern* ra) (routes ~@body)))

(defn$ -domain [Regex StateFn => StateFn]
  [pattern f]
  (fn [state0]
    (maybe-> [sn (-> state0 :routing/request :server-name)
              _ (match-glob pattern sn)]
      (let [[v state1] (f state0)]
        (if (:routing/matched? state1)
          [v state1]
          [resp/empty-resp state0]))
      [resp/empty-resp state0])))

(defmacro domain
  [d & body]
  `(-domain ~(compile-pattern* d) (routes ~@body)))

(defn$ -pred [(IFn [String => Any]) StateFn => StateFn]
  [p f]
  (fn [state0]
    (if-let [result (p (next-path-segment state0))]
      (let [state1 (consume-path-segment state0)
            [v state2] ((f result) state1)]
        (if (:routing/matched? state2)
          [v state2]
          [resp/empty-resp state1]))
      [resp/empty-resp state0])))

(defmacro pred
  [p bindings & body]
  `(-pred ~p (fn ~(mk-bindings bindings) (routes ~@body))))

(defn$ guard ([Boolean => StateFn] [Boolean String => StateFn])
  ([check]
     (fn [state]
       (if-not check
         [(resp/forbidden) (set-matched state)]
         [resp/empty-resp state])))
  ([check msg]
     (fn [state]
       (if-not check
         [(resp/text-response 403 msg) (set-matched state)]
         [resp/empty-resp state]))))

(defn$ terminate [Any => StateFn]
  [v]
  (fn [state]
    [v (set-matched state)]))

(defn$ -truncate [StateFn => StateFn]
  [f]
  (fn [state0]
    (let [state1 (assoc state0 :routing/path-segments (list))
          [v state2] (f state1)]
      (if (:routing/matched? state2)
        [v state2]
        [v (set-matched state1)]))))

(defmacro truncate
  [& body]
  `(-truncate (routes ~@body)))

(defn$ -index [StateFn => StateFn]
  [f]
  (fn [state0]
    (if (path-consumed? state0)
      (let [[v state1] (f state0)]
        (if (:routing/matched? state1)
          [v state1]
          [v (set-matched state0)]))
      [resp/empty-resp state0])))

(defmacro index
  [& body]
  `(-index (routes ~@body)))

(defn$ static [#{String} FileOptions => StateFn]
  [segments opts]
  (fn [state]
    (if (and (= (:routing/request-method state) :get)
             (get segments (next-path-segment state)))
      (if-let [r (file-response (-> state :routing/request :uri) opts)]
        [(resp/map->Response r) (set-matched state)]
        [resp/empty-resp state])
      [resp/empty-resp state])))

(defn$ -request [StateFn => StateFn]
  [f]
  (fn [state]
    ((f (:routing/request state) state))))

(defmacro request
  [bindings & body]
  `(-request (fn ~bindings (routes ~@body))))

(defn$ -headers [StateFn => StateFn]
  [f]
  (fn [state]
    ((f (-> state :routing/request :headers)) state)))

(defmacro headers
  [bindings & body]
  `(-headers (fn ~bindings (routes ~@body))))

(defn$ -body [StateFn => StateFn]
  [f]
  (fn [state]
    ((f (-> state :routing/request :body)) state)))

(defmacro body
  [bindings & body]
  `(-body (fn ~bindings (routes ~@body))))

(defmacro resp
  [form]
  `(fn [state#]
     (if (path-consumed? state#)
       [~form (set-matched state#)]
       [resp/empty-resp state#])))

(defn$ split-path [String => (LazySeq String)]
  {:private true}
  [uri]
  (->> (string/split uri #"/")
       (remove string/blank?)))

(defn$ init-state [Request => State]
  [req]
  {:routing/path-segments (-> req :uri split-path)
   :routing/request-method (:request-method req)
   :routing/request req
   :routing/middleware {}
   :routing/matched? false})

(defmacro router
  "Returns a function that takes some initial state and
  returns a function that takes a request map and returns
  a response."
  [& body]
  `(fn [state0#]
     (fn [req#]
       (let [state1# (merge (init-state req#) state0#)
             [v# state2#] (reduce-forms state1# (list ~@body))
             resp# (if (:routing/matched? state2#)
                     (resp/respond v#)
                     (resp/not-found))]
         ;; Copy middleware to response object, then convert to a map
         (->> (:routing/middleware state2#)
              (assoc resp# :routing/middleware)
              (into {}))))))

(defmacro defrouter
  "Define a router function."
  [n & body]
  `(def ~n (router ~@body)))
