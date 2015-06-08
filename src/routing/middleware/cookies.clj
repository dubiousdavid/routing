(ns routing.middleware.cookies
  "Meant to be aliased when requiring."
  (:use ring.middleware.cookies
        [annotate.fns :only [defn$]]
        [annotate.types :only [Option Fn Named Any Map]]
        routing.types)
  (:require [routing.util :as util])
  (:refer-clojure :exclude [get remove]))

(defn$ decode ([=> StateFn] [Map => StateFn])
  "Decode cookies and put in the request map."
  ([] (decode {}))
  ([opts]
     (fn [state]
       [nil (update-in state [:routing/request] cookies-request opts)])))

(defn$ encode ([Response => Response] [Response Map => Response])
  "Encode cookies and put in the response map."
  ([resp] (encode resp {}))
  ([resp opts]
   (if (or (contains? (:routing/middleware resp) :cookies)
           (contains? resp :cookies))
     (let [cookies (merge (:cookies resp)
                          (get-in resp [:routing/middleware :cookies]))]
       (-> resp
           (util/dissoc-in [:routing/middleware :cookies])
           (assoc :cookies cookies)
           (cookies-response opts)))
     resp)))

(defn$ get [Request Named => String]
  "Get the value of the cookie with key k."
  [req k]
  (get-in req [:cookies (name k)]))

(defn$ mk-cookie ([Any => Cookie] [Any (Option CookieOpts) => Cookie])
  ([val] (mk-cookie val nil))
  ([val opts]
   (merge {:value (str val) :path "/"} opts)))

(defn$ put ([Any Any => StateFn]
            [Any Any (Option CookieOpts) => StateFn])
  "Write a cookie with key k and value v. Optionally pass
  a map of cookie attributes."
  ([k v] (put k v nil))
  ([k v opts]
   (fn [state]
     [nil (->> (mk-cookie v opts)
               (assoc-in state [:routing/middleware :cookies (name k)]))])))

(defn$ remove ([Any => StateFn] [Any CookieOpts => StateFn])
  "Remove the cookie with key k. Optionally pass a
  map of cookie attributes."
  ([k] (remove k nil))
  ([k opts]
     (put k nil (merge {:expires "Thu, 01-Jan-1970 00:00:01 GMT"} opts))))
