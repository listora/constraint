(ns constraint.coerce.json
  (:require [constraint.coerce :refer (make-coercion)]))

(def uuid-pattern
  #"[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")

(def string->uuid
  (make-coercion [String java.util.UUID]
    :validate #(re-matches uuid-pattern %)
    :coerce   #(java.util.UUID/fromString %)
    :schema   {"type" "string", "format" "uuid"}))

(def date-time-pattern
  #"^\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d([+-][0-2]\d:[0-5]\d|Z)$")

(def string->date
  (make-coercion [String java.util.Date]
    :validate #(re-matches date-time-pattern %)
    :coerce   #(.getTime (javax.xml.bind.DatatypeConverter/parseDateTime %))
    :schema   {"type" "string", "format" "date-time"}))

(def type-coercions
  "A set of standard type coercion rules for data parsed from JSON."
  {java.util.UUID string->uuid
   java.util.Date string->date})
