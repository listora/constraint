(ns constraint.coerce
  (:require [constraint.validate :refer (Validate validate*)]))

(defprotocol Coerce
  (coerce [coercion data]))

(defprotocol Coercion
  (coercion [constraint mapping]))

(def noop
  (reify
    Coerce
    (coerce [_ data] data)
    Validate
    (validate* [_ _] '())))

(extend-type Class
  Coercion
  (coercion [type mapping] (mapping type)))

(extend-type Object
  Coercion
  (coercion [_ _] noop))

(defn failed-coercion [type data]
  {:error    :failed-coercion
   :coercion type
   :found    data})

(defn make-coercion
  [[from to] & {valid-fn :valid?, coerce-fn :coerce}]
  (reify
    Coerce
    (coerce [_ data] (coerce-fn data))
    Validate
    (validate* [_ data]
      (or (seq (validate* from data))
          (if-not (valid-fn data)
            [(failed-coercion to data)])))))

(def uuid-pattern
  #"[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")

(def string->uuid
  (make-coercion [String java.util.UUID]
    :valid? #(re-matches uuid-pattern %)
    :coerce #(java.util.UUID/fromString %)))

(def json
  {java.util.UUID string->uuid})
