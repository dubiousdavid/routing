# routing

Ring-compatible HTTP routing library.

Objectives:

1. Flexible routing. ✓
2. Propogation of application state without the use of global vars. ✓
3. Selective application of middleware. ✓

## Installation

```clojure
[com.2tothe8th/routing "0.1.0"]
```

## API

http://dubiousdavid.github.io/routing/

## How to use

Routes are split into segment paths, where each segment is separated by a forward slash in the request URL. If a route consumes all the segments then the body of that route is returned as the response.

Routing is totally pure, that is there are no side effects. Assuming that the body of your routes are also pure, or you are able to reproduce a consistent app state, you should be able to create dependable tests around your routes.

Furthermore, middleware is designed to be dependably testable as well. Take for example logging. The side-effect of logging does not take place until after the route has been selected, allowing you to test what log messages would be produced in a purely functional manner.

Below is a fairly comprehensive example of how to use this library. To see what the output would be for a given route, look at the expected test result in [test/routing.examples-test.clj](https://github.com/dubiousdavid/routing/blob/master/test/routing/examples_test.clj).

```clojure
(ns my-ns
  (:require [routing.core :refer :all]
            [routing.middleware.cookies :as cookies]
            [ring.middleware.session :refer [wrap-session]]
            [routing.middleware.params :as params]
            [routing.middleware.logging :as log]
            [routing.middleware :as mw]
            [routing.response :refer [ok]]
            [monads.state :refer [asks]]))

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
```
