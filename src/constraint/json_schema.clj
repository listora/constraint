(ns constraint.json-schema
  (:require [constraint.core :refer (many? optional?)]))

(defprotocol JsonSchema
  (json-schema* [definition]))

(defn json-schema
  "Return a JSON schema that is equivalent to the supplied constraint."
  [definition]
  (-> (json-schema* definition)
      (assoc "$schema" "http://json-schema.org/draft-04/schema#")))

(extend-type constraint.core.AnyType
  JsonSchema
  (json-schema* [_] {}))

(extend-type constraint.core.Union
  JsonSchema
  (json-schema* [definition]
    {"oneOf" (mapv json-schema* (.constraints definition))}))

(extend-type constraint.core.Intersection
  JsonSchema
  (json-schema* [definition]
    {"allOf" (mapv json-schema* (.constraints definition))}))

(extend-type constraint.core.SizeBounds
  JsonSchema
  (json-schema* [definition]
    (let [min (.min definition)
          max (.max definition)]
      (merge {"maxItems" max}
             (if (zero? min) {} {"minItems" min})))))

(extend-type Class
  JsonSchema
  (json-schema* [definition]
    {"type" (condp #(isa? %2 %1) definition
              Integer       "integer"
              Long          "integer"
              BigInteger    "integer"
              Number        "number"
              String        "string"
              Boolean       "boolean"
              java.util.Map "object"
              Iterable      "array")}))

(defn- single? [x]
  (not (or (many? x) (optional? x))))

(defn- constraint [x]
  (if (single? x) x (.constraint x)))

(extend-type clojure.lang.IPersistentVector
  JsonSchema
  (json-schema* [definition]
    (cond
     (and (= (count definition) 1) (many? (first definition)))
     {"type" "array"
      "items" (json-schema* (constraint definition))}

     (every? single? definition)
     {"type" "array"
      "items" (mapv json-schema* (constraint definition))}

     (and (every? single? (butlast definition)) (many? (last definition)))
     {"type" "array"
      "items" (vec (map json-schema* (butlast definition)))
      "additionalItems" (json-schema* (constraint (last definition)))}

     :else
     {"type" "array"
      "items" {"oneOf" (vec (set (map (comp json-schema* constraint) definition)))}})))

(extend-type clojure.lang.IPersistentMap
  JsonSchema
  (json-schema* [definition]
    {"type" "object"
     "additionalProperties" false
     "properties" (into {} (for [[k v] definition]
                             [(name k) (json-schema* v)]))}))

(extend-type java.util.regex.Pattern
  JsonSchema
  (json-schema* [definition]
    {"pattern" (str definition)}))

(extend-protocol JsonSchema
  nil
  (json-schema* [_] {"enum" [nil]})
  Object
  (json-schema* [value] {"enum" [value]}))
