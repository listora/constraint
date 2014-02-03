(ns constraint.coerce
  (:require [constraint.validate :refer (Validate validate* walk-data)]
            [constraint.core :refer (U I & ?)]))

(defprotocol Coerce
  (coerce* [definition data]))

(extend-protocol Coerce
  Object
  (coerce* [_ data] data))

(defn coerce [definition data]
  (walk-data coerce* definition data))

(defprotocol Walk
  (postwalk* [form f]))

(extend-protocol Walk
  constraint.core.Union
  (postwalk* [u f] (apply U (map #(postwalk* % f) (.constraints u))))
  constraint.core.Intersection
  (postwalk* [i f] (apply I (map #(postwalk* % f) (.constraints i))))
  constraint.core.Many
  (postwalk* [m f] (& (postwalk* (.constraint m) f)))
  constraint.core.Optional
  (postwalk* [o f] (? (postwalk* (.constraint o) f)))
  clojure.lang.IPersistentVector
  (postwalk* [v f] (f (mapv #(postwalk* % f) v)))
  clojure.lang.IPersistentMap
  (postwalk* [m f]
    (f (into {} (for [[k v] m] (f [(postwalk* k f) (postwalk* v f)])))))
  Object
  (postwalk* [x f] (f x)))

(defn postwalk
  "Performs a depth-first, post-order traversal of a data structure.
  Calls f on each sub-form,uses f's return value in place of the original."
  [f form]
  (postwalk* form f))

(defn coercion
  "Apply coercions to a constraint based on a map of rules."
  [constraint rules]
  (postwalk (fn [x] (rules x x)) constraint))

(defn failed-coercion [type data]
  {:error    :failed-coercion
   :coercion type
   :found    data})

(defn make-coercion
  "Construct a coercion that maps data of one type to another type. Takes a
  :validate predicate to determine if the data can be coerced, and a :coerce
  function, which returns the coerced value."
  [[in-type out-type] & {valid-fn :validate, coerce-fn :coerce}]
  (reify
    Coerce
    (coerce* [_ data] (coerce-fn data))
    Validate
    (validate* [_ data]
      (or (seq (validate* in-type data))
          (if-not (valid-fn data)
            [(failed-coercion out-type data)])))))

(def uuid-pattern
  #"[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")

(def string->uuid
  (make-coercion [String java.util.UUID]
    :validate #(re-matches uuid-pattern %)
    :coerce   #(java.util.UUID/fromString %)))

(def json
  {java.util.UUID string->uuid})
