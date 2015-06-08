(ns routing.generate
  (:require routing.examples example.protocols)
  (:use example.tests.midje)
  (:import java.io.File))

(defn- unit-tests [f]
  (extend-protocol example.protocols/Printable
    File
    (printd [this]
      (list 'File. (.toString this))))

  (f 'routing.examples
     :import '[java.io.File]
     :use '[[routing.response :only [map->Response]]]))

(defn preview-unit-tests []
  (unit-tests gen-facts))

(defn gen-unit-tests []
  (unit-tests gen-facts-file))
