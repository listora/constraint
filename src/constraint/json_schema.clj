(ns constraint.json-schema
  (:require [constraint.core :refer (many? optional?)]
            [constraint.validations]))

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

(extend-type constraint.core.Description
  JsonSchema
  (json-schema* [definition]
    (assoc (json-schema* (.constraint definition)) "doc" (.doc definition))))

(defn- enum? [x]
  (and (map? x) (contains? x "enum") (= (count x) 1)))

(defn- merge-enums [schemas]
  (if (some enum? schemas)
    (concat (remove enum? schemas)
            [{"enum" (->> schemas (filter enum?) (mapcat #(get % "enum"))
                          set sort vec)}])
    schemas))

(extend-type constraint.core.Union
  JsonSchema
  (json-schema* [definition]
    (let [schemas (merge-enums (mapv json-schema* (.constraints definition)))]
      (if (> (count schemas) 1)
        {"oneOf" (vec schemas)}
        (first schemas)))))

(defn- assoc-in-available [schemas [k v]]
  (lazy-seq
   (if (seq schemas)
     (let [s (first schemas)]
       (if (contains? s k)
         (cons s (assoc-in-available (rest schemas) [k v]))
         (cons (assoc s k v) (rest schemas))))
     (list {k v}))))

(defn- merge-interactions [schemas]
  (reduce assoc-in-available [] (set (apply concat schemas))))

(defn- move [m k1 k2]
  (if (contains? m k1)
    (-> m (dissoc k1) (assoc k2 (m k1)))
    m))

(defn- correct-size-bounds [schema]
  (if (= (schema "type") "string")
    (-> schema (move "maxItems" "maxLength") (move "minItems" "minLength"))
    schema))

(extend-type constraint.core.Intersection
  JsonSchema
  (json-schema* [definition]
    (let [schemas (->> (map json-schema* (.constraints definition))
                       (merge-interactions)
                       (map correct-size-bounds))]
      (if (> (count schemas) 1)
        {"allOf" (vec schemas)}
        (first schemas)))))

(extend-protocol JsonSchema
  constraint.validations.Minimum
  (json-schema* [definition] {"minimum" (.min definition)})

  constraint.validations.Maximum
  (json-schema* [definition] {"maximum" (.max definition)})

  constraint.validations.MinSize
  (json-schema* [definition] {"minItems" (.min definition)})

  constraint.validations.MaxSize
  (json-schema* [definition] {"maxItems" (.max definition)}))

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
      "items" (json-schema* (constraint (first definition)))}

     (every? single? definition)
     {"type" "array"
      "items" (mapv json-schema* (constraint definition))}

     (and (every? single? (butlast definition)) (many? (last definition)))
     {"type" "array"
      "items" (vec (map json-schema* (butlast definition)))
      "additionalItems" (json-schema* (constraint (last definition)))}

     :else
     {"type" "array"
      "items" {"oneOf" (vec (set (map (comp json-schema* constraint)
                                      definition)))}})))

(defn- map-key? [x]
  (let [x (constraint x)]
    (or (string? x) (symbol? x) (keyword? x))))

(defn- required-keys [definition]
  (->> (keys definition)
       (filter map-key?)
       (remove optional?)
       (map name)))

(extend-type clojure.lang.IPersistentMap
  JsonSchema
  (json-schema* [definition]
    (merge
     {"type" "object"
      "additionalProperties"
      (let [ks (keys definition)]
        (if (some many? ks)
          (json-schema* (val (first (filter (comp many? key) definition))))
          false))
      "properties"
      (into {} (for [[k v] definition :when (map-key? k)]
                 [(name (constraint k)) (json-schema* v)]))}
     (if-let [required (seq (required-keys definition))]
       {"required" (vec required)}))))

(extend-type java.util.regex.Pattern
  JsonSchema
  (json-schema* [definition]
    {"type" "string", "pattern" (str definition)}))

(extend-protocol JsonSchema
  nil
  (json-schema* [_] {"enum" [nil]})
  clojure.lang.Keyword
  (json-schema* [value] {"enum" [(name value)]})
  clojure.lang.Symbol
  (json-schema* [value] {"enum" [(name value)]})
  Object
  (json-schema* [value] {"enum" [value]}))
