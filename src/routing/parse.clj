(ns routing.parse
  (:import org.joda.time.LocalDate java.util.Locale))

(defn parse-int [n]
  (Integer/parseInt n))

(defn parse-date
  "Parse a date with format yyyy-MM-dd. Returns a LocalDate."
  [date]
  (LocalDate. date))

(defn parse-uuid [uuid]
  (java.util.UUID/fromString uuid))

(defn match-regex [regex]
  (partial re-matches regex))

(defn parse-locale [locale]
  (Locale/forLanguageTag locale))

(def parsers
  {:int [#"\d+" parse-int]
   :locale [#"[a-zA-Z]{2,8}-([a-zA-Z]{2}|[0-9]{3})" parse-locale]
   :slug [#"[a-zA-Z0-9_-]+" identity]
   :date [#"[0-9]{4}-[0-9]{2}-[0-9]{2}" parse-date]
   :uuid [#"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"
           parse-uuid]
   :raw [#"" identity]})
