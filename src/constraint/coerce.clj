(ns constraint.coerce
  (:require [constraint.core :refer (U I & ? constraint)]
            [constraint.validate :refer (Validate validate* walk-data)]
            [constraint.json-schema :refer (JsonSchema)]))

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
  (postwalk* [m f] (& (postwalk* (constraint m) f)))
  constraint.core.Optional
  (postwalk* [o f] (? (postwalk* (constraint o) f)))
  clojure.lang.IPersistentVector
  (postwalk* [v f] (f (mapv #(postwalk* % f) v)))
  clojure.lang.IPersistentMap
  (postwalk* [m f]
    (f (into {} (for [[k v] m] (f [(postwalk* k f) (postwalk* v f)])))))
  constraint.core.Constraint
  (postwalk* [c f] (postwalk* (constraint c) f))
  Object
  (postwalk* [x f] (f x)))

(defn postwalk
  "Performs a depth-first, post-order traversal of a data structure.
  Calls f on each sub-form,uses f's return value in place of the original."
  [f form]
  (postwalk* form f))

(defn transform
  "Transform a constraint based on a series of rules. Rules may be represented
  as a map or a function. In both cases, rules should map an existing value to
  a new value."
  [constraint & rules]
  (postwalk #(reduce (fn [x r] (if (map? r) (r x x) (r x))) % rules) constraint))

(defn make-coercion
  "Construct a coercion that maps data of one type to another type. Takes a
  :validate predicate to determine if the data can be coerced, and a :coerce
  function, which returns the coerced value."
  [[in-type out-type] & {:keys [validate coerce schema]}]
  (reify
    Coerce
    (coerce* [_ data] (coerce data))
    JsonSchema
    (json-schema* [_] schema)
    Validate
    (validate* [_ data]
      (or (seq (validate* in-type data))
          (if-not (validate data)
            [{:error    :failed-coercion
              :coercion out-type
              :found    data}])))))
