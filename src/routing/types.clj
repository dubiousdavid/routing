(ns routing.types
  (:use annotate.types)
  (:import [java.io File InputStream]
           [org.joda.time Interval DateTime]))

(def RequestMethod
  (U :get :head :options :put :post :delete))

(def Scheme (U :http :https))

(def Request
  {:uri String
   :request-method RequestMethod
   :server-name String
   :scheme Scheme
   :headers {String String}})

(def State
  {:routing/path-segments (Seq String)
   :routing/request-method Keyword
   :routing/request Request
   :routing/middleware Map
   :routing/matched? Boolean})

(def StateFn
  (IFn [State => [Any State]]))

(def Response
  {:status Int
   :headers Map
   :body (U String File InputStream)})

(def FileOptions
  {:root String})

(def CookieOpts
  {(optional-key :path) String
   (optional-key :domain) String
   (optional-key :max-age) (U Interval Int)
   (optional-key :expires) (U DateTime String)
   (optional-key :secure) Boolean
   (optional-key :http-only) Boolean})

(def Cookie
  (assoc CookieOpts :value String))

(def SessionOpts
  {(optional-key :store) Fn
   (optional-key :root) String
   (optional-key :cookie-name) String
   (optional-key :cookie-attrs) CookieOpts})
