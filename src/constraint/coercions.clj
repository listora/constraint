(ns constraint.coercions
  "Common-place type coercions.")

(defn- failed-coercion [type data]
  {:error    :failed-coercion
   :coercion type
   :found    data})

(def uuid-pattern
  #"[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")

(defn string->uuid [s]
  (if (re-matches uuid-pattern s)
    {:value (java.util.UUID/fromString s)}
    {:errors #{(failed-coercion java.util.UUID s)}}))

(def date-time-pattern
  #"^\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d([+-][0-2]\d:[0-5]\d|Z)$")

(defn string->date [s]
  (if (re-matches date-time-pattern s)
    {:value (.getTime (javax.xml.bind.DatatypeConverter/parseDateTime s))}
    {:errors #{(failed-coercion java.util.Date s)}})) 

(def json-coercions
  "A set of standard type coercion rules for data parsed from JSON."
  {[String java.util.UUID] string->uuid
   [String java.util.Date] string->date})
