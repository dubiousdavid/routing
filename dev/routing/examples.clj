(ns routing.examples
  (:use routing.core
        [routing.response :only [empty-resp with-edn-formatting]]
        example.core
        [monads.state :only [asks]])
  (:require [clojure.string :as string]
            [routing.middleware.cookies :as cookies]
            [ring.middleware.session :refer [wrap-session]]
            [routing.middleware.params :as params]
            [routing.middleware.logging :as log]
            [routing.middleware :as mw]
            [routing.response :refer [ok]]))

(defn req
  [uri & {:keys [domain scheme method query-string]
          :or {domain "example.org"
               scheme :http
               method :get}}]
  {:uri uri
   :request-method method
   :server-name domain
   :scheme scheme
   :headers {}
   :query-string query-string})

(def posts
  {1234 "Super helpful blog post"})

(def comments
  {1234 ["So incredibly helpful" "Very helpful"]})

(def public-dir "resources/public")
(def admin-access false)

(def faqs
  {"how-to-post-comment" "How to post a comment"
   "how-to-remove-comment" "How to remove a comment"})

(def posts-by-country
  {"US" posts})

(defn connect-to-db [] {:host "localhost" :port 1000})
(defn connect-to-email [] {:host "localhost" :port 2000})
(defn insert-row [db & args] (assert db) 1)
(defn fetch-row [db & args]
  (assert db)
  {:user-id 1
   :first-name "Billy"
   :last-name "Bob"})
(defn remove-row [db query id] (assert db) (= id 1))

(defn init-app-state []
  {:db (connect-to-db)
   :email (connect-to-email)})

(defn add-user [db first-name last-name]
  (insert-row db "INSERT INTO users (?, ?)" [first-name last-name]))

(defn get-user [db user-id]
  (fetch-row db "SELECT * FROM users where user_id = ?" user-id))

(defn remove-user [db user-id]
  (remove-row db "DELETE FROM users where user_id = ?" user-id))

(defroutes users
  (path "users"
    (state-routes [db (asks (comp :db :app))]
      (GET-int [id]
        (resp (get-user db id)))
      (POST [{params :params}]
        (let-routes [{:strs [first-name last-name]} params]
          (resp (add-user db first-name last-name))))
      (DELETE [{params :params}]
        (let-routes [{id "id"} params
                     id (Integer/parseInt id)]
          (resp (if (remove-user db id)
                  "User successfully deleted"
                  "User not found")))))))

(defroutes frontend
  (domain "example.org"
    (log/info "example.org domain")
    (index "Home page")
    (path "blog"
      (log/info "Blog path")
      (match-int [post-id]
        (path "comments"
          (log/info "Comments path")
          (match-int [comment-id]
            (get-in comments [post-id comment-id]))
          (resp (comments post-id)))
        (get posts post-id))
      (match-date [date]
        (str "Blog post on " date))
      (param [#"[A-Z]{2}" identity] [country-code]
        (posts-by-country country-code))
      (resp "Blog"))
    (path "req"
      (request [req] req))
    (path "faq"
      (pred faqs [faq]
        (str "FAQ: " faq)))))

(defroutes api
  (domain "api.*"
    (https
      (path "posts"
        (match-uuid [uuid]
          (let-routes [uuid (.toString uuid)]
            (GET [] (str "Post " uuid))
            (POST [req] (str "Post " uuid " added"))
            (PUT [req] (str "Post  " uuid " updated"))))
        posts))))

(defroutes admin
  (domain "admin.example.org"
    (params/decode)
    (cookies/decode)
    users
    (path "super-secret"
      (guard admin-access "You shall not pass!"))
    (path "black-hole"
      (terminate "You got sucked in.")
      (resp "Made it out."))
    (path "view"
      (pred #{"monkeys" "cheetahs"} [animal]
        (cookies/put :warning "Animals have escaped the zoo!")
        (-> (ok "I'm sorry, nothing to see here.")
            (assoc :session {:viewed animal})
            resp)))
    (path "viewed"
      (GET [req] (:session req)))
    (remote-address "216.*"
      (resp "You may enter sir."))
    (match-regex #"launch-(.+)" [[_ launched-item]]
      (cookies/put :missle-launched "It's a nuke!")
      (resp (str "You launched " launched-item "!")))
    (path "logout"
      (-> (ok "Successfully logged out!")
          (assoc :session nil)
          resp))
    (resp "Welcome to the admin portal!")))

(defrouter app-router
  (static #{"images" "css" "js"} {:root public-dir})
  frontend
  api
  admin)

(def handler (app-router {:app (init-app-state)}))
(def app (comp mw/cleanup log/log! cookies/encode (wrap-session handler)))

(ex (handler (req "/")))
(ex (handler (req "/blog")))
(ex (handler (req "/blog/1234")))
(ex (handler (req "/blog/US")))
(ex (handler (req "/blog/2014-01-01")))
(ex (handler (req "/blog/1234/comments")))
(ex (handler (req "/blog/1234/comments/0")))
(ex (handler (req "/faq/how-to-post-comment")))
(ex (handler (req "/faq/how-to-remove-comment")))
(ex (handler (req "/posts" :scheme :https :domain "api.example.org")))
(ex (handler (req "/posts/64dbe8a0-4cd7-11e3-8f96-0800200c9a66"
              :scheme :https :domain "api.example.org")))
(ex (handler (req "/posts/64dbe8a0-4cd7-11e3-8f96-0800200c9a66" :method :post
              :scheme :https :domain "api.example.org")))
(ex (handler (req "/posts/64dbe8a0-4cd7-11e3-8f96-0800200c9a66" :method :put
              :scheme :https :domain "api.example.org")))
(ex (:body (handler (req "/" :domain "admin.example.org"))))
(ex (:body (handler (req "/super-secret" :domain "admin.example.org"))))
(ex (:body (handler (req "/black-hole" :domain "admin.example.org"))))
(ex (:body (handler (req "/users/1" :domain "admin.example.org"))))
(ex (:body (handler (req "/users" :query-string "firstname=Billy&lastname=Bob"
                   :domain "admin.example.org" :method :post))))
(ex (:body (handler (req "/users" :query-string "id=1" :domain "admin.example.org"
                   :method :delete))))
(ex (handler (req "/view/monkeys" :domain "admin.example.org")))
(ex (-> (handler (req "/launch-missles" :domain "admin.example.org")) :routing/middleware :cookies))
(ex (handler (req "/images/clojure-icon.gif")))
