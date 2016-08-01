(defproject com.2tothe8th/routing "0.1.0"
  :description "Ring-compatible HTTP routing library."
  :url "https://github.com/dubiousdavid/routing"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.4.0"]
                 [com.roomkey/annotate "1.0.1"]
                 [com.2tothe8th/example "0.4.0"]
                 [cheshire "5.6.1"]
                 [stch-library/glob "0.3.0"]
                 [com.2tothe8th/monads "0.2.0"]
                 [org.clojure/tools.logging "0.3.1"]]
  :profiles {:dev {:jvm-opts ["-Dannotate.typecheck=on"]
                   :dependencies [[midje "1.8.3"]
                                  [ring-undertow-adapter "0.2.2"]]
                   :source-paths ["dev"]}}
  :codox {:src-dir-uri "https://github.com/dubiousdavid/routing/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "."
          :exclude [routing.examples
                    routing.generate
                    routing.server]})
