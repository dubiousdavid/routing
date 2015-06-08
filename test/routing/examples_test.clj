(ns routing.examples-test
  (:import java.io.File)
  (:use [routing.response :only [map->Response]] midje.sweet routing.examples))

(fact (handler (req "/")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "Home page",
   :routing/middleware {:logs [[:info "example.org domain"]]}})
(fact (handler (req "/blog")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "Blog",
   :routing/middleware
   {:logs [[:info "example.org domain"] [:info "Blog path"]]}})
(fact (handler (req "/blog/1234")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "Super helpful blog post",
   :routing/middleware
   {:logs [[:info "example.org domain"] [:info "Blog path"]]}})
(fact (handler (req "/blog/US")) =>
  {:status 200,
   :headers {"Content-Type" "application/json"},
   :body "{\"1234\":\"Super helpful blog post\"}",
   :routing/middleware
   {:logs [[:info "example.org domain"] [:info "Blog path"]]}})
(fact (handler (req "/blog/2014-01-01")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "Blog post on 2014-01-01",
   :routing/middleware
   {:logs [[:info "example.org domain"] [:info "Blog path"]]}})
(fact (handler (req "/blog/1234/comments")) =>
  {:status 200,
   :headers {"Content-Type" "application/json"},
   :body "[\"So incredibly helpful\",\"Very helpful\"]",
   :routing/middleware
   {:logs
    [[:info "example.org domain"]
     [:info "Blog path"]
     [:info "Comments path"]]}})
(fact (handler (req "/blog/1234/comments/0")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "So incredibly helpful",
   :routing/middleware
   {:logs
    [[:info "example.org domain"]
     [:info "Blog path"]
     [:info "Comments path"]]}})
(fact (handler (req "/faq/how-to-post-comment")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "FAQ: How to post a comment",
   :routing/middleware {:logs [[:info "example.org domain"]]}})
(fact (handler (req "/faq/how-to-remove-comment")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "FAQ: How to remove a comment",
   :routing/middleware {:logs [[:info "example.org domain"]]}})
(fact (handler (req "/posts" :scheme :https :domain "api.example.org")) =>
  {:status 200,
   :headers {"Content-Type" "application/json"},
   :body "{\"1234\":\"Super helpful blog post\"}",
   :routing/middleware {}})
(fact (handler (req "/posts/64dbe8a0-4cd7-11e3-8f96-0800200c9a66" :scheme :https :domain "api.example.org")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "Post 64dbe8a0-4cd7-11e3-8f96-0800200c9a66",
   :routing/middleware {}})
(fact (handler (req "/posts/64dbe8a0-4cd7-11e3-8f96-0800200c9a66" :method :post :scheme :https :domain "api.example.org")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "Post 64dbe8a0-4cd7-11e3-8f96-0800200c9a66 added",
   :routing/middleware {}})
(fact (handler (req "/posts/64dbe8a0-4cd7-11e3-8f96-0800200c9a66" :method :put :scheme :https :domain "api.example.org")) =>
  {:status 200,
   :headers {"Content-Type" "text/html"},
   :body "Post  64dbe8a0-4cd7-11e3-8f96-0800200c9a66 updated",
   :routing/middleware {}})
(fact (:body (handler (req "/" :domain "admin.example.org"))) =>
  "Welcome to the admin portal!")
(fact (:body (handler (req "/super-secret" :domain "admin.example.org"))) =>
  "You shall not pass!")
(fact (:body (handler (req "/black-hole" :domain "admin.example.org"))) =>
  "You got sucked in.")
(fact (:body (handler (req "/users/1" :domain "admin.example.org"))) =>
  "{\"last-name\":\"Bob\",\"user-id\":1,\"first-name\":\"Billy\"}")
(fact (:body (handler (req "/users" :query-string "firstname=Billy&lastname=Bob" :domain "admin.example.org" :method :post))) =>
  "1")
(fact (:body (handler (req "/users" :query-string "id=1" :domain "admin.example.org" :method :delete))) =>
  "User successfully deleted")
(fact (handler (req "/view/monkeys" :domain "admin.example.org")) =>
  {:body "I'm sorry, nothing to see here.",
   :headers {"Content-Type" "text/html"},
   :session {:viewed "monkeys"},
   :status 200,
   :routing/middleware {:cookies {"warning" {:path "/", :value "Animals have escaped the zoo!"}}}})
(fact (-> (handler (req "/launch-missles" :domain "admin.example.org")) :routing/middleware :cookies) =>
  {"missle-launched" {:value "It's a nuke!", :path "/"}})
(fact (handler (req "/images/clojure-icon.gif")) =>
  {:status 200,
   :headers
   {"Last-Modified" "Mon, 08 Jun 2015 15:02:44 GMT",
    "Content-Length" "2174"},
   :body (File. "resources/public/images/clojure-icon.gif"),
   :routing/middleware {}})
