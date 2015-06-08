(ns routing.util
  (:use [annotate.fns :only [defn$]]
        [annotate.types :only [Map Any CanSeq]]))

(defn inspect
  "Prints the current state and returns it unmodified."
  [state]
  (println state)
  [nil state])

(defn path-consumed? [state]
  (empty? (:routing/path-segments state)))

(defn consume-path-segment [state]
  (update-in state [:routing/path-segments] rest))

(defn next-path-segment [state]
  (first (:routing/path-segments state)))

(defn set-matched [state]
  (assoc state :routing/matched? true))

(defn$ dissoc-in [Map (CanSeq) => Map]
  "Dissociates an entry from a nested associative
  structure returning a new nested structure. keys is
  a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))
