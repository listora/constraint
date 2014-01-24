(ns constraint.json-schema
  (:require constraint.core
            [constraint.internal.parse :refer [split-vector]]))

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

(defn- move [m k1 k2]
  (if (contains? m k1)
    (-> m (dissoc k1) (assoc k2 (m k1)))
    m))

(defn- correct-bounds [schema]
  (if (= (schema "type") "string")
    (-> schema
        (move "maxItems" "maxLength")
        (move "minItems" "minLength"))
    schema))

(extend-type constraint.core.Intersection
  JsonSchema
  (json-schema* [definition]
    (->> (.constraints definition)
         (map json-schema*)
         (apply merge)
         (correct-bounds))))

(extend-type constraint.core.SizeBounds
  JsonSchema
  (json-schema* [_]
    (merge {"maxItems" max}
           (if (zero? min) {} {"minItems" min}))))

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

(extend-type clojure.lang.IPersistentVector
  JsonSchema
  (json-schema* [definition]
    (let [[items add-items] (split-vector definition)]
      (if (empty? items)
        {"type" "array", "items" (json-schema* add-items)}
        {"type" "array"
         "items" (mapv json-schema* items)
         "additionalItems" (if (empty? add-items)
                             false
                             (json-schema* add-items))}))))

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
