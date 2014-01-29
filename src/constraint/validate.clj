(ns constraint.validate
  "Validate a data structure against a constraint."
  (:require [constraint.core :refer (many? optional?)]))

(defprotocol Validate
  (validate* [definition data]))

(def messages
  {:invalid-type "data type does not match definition"
   :invalid-value "data value does not match definition"
   :no-valid-constraint "no valid constraint in union"
   :size-out-of-bounds "data size is out of bounds"
   :pattern-not-matching "data does not match regular expression in definition"})

(defn validate
  "Validate a data structure based on a constraint. If the data structure is
  valid, an empty collection is returned. If the data is invalid, a collection
  of errors is returned."
  [definition data]
  (for [error (validate* definition data)]
    (assoc error :message (messages (:error error)))))

(defn valid?
  "Return true if the data structure is valid according to the supplied
  constraint, or false if it is not."
  [definition data]
  (empty? (validate* definition data)))

(extend-type constraint.core.AnyType
  Validate
  (validate* [_ _] '()))

(extend-type constraint.core.Description
  Validate
  (validate* [definition data] (validate* (.constraint definition) data)))

(extend-type constraint.core.Union
  Validate
  (validate* [definition data]
    (let [errors (map #(validate % data) (.constraints definition))]
      (if-not (some empty? errors)
        [{:error    :no-valid-constraint
          :failures (apply concat errors)}])))) 

(extend-type constraint.core.Intersection
  Validate
  (validate* [definition data]
    (vec (set (mapcat #(validate % data) (.constraints definition))))))

(extend-type constraint.core.SizeBounds
  Validate
  (validate* [definition data]
    (let [min (.min definition)
          max (.max definition)]
      (if-let [n (try (count data) (catch Throwable _ nil))]
        (if-not (<= min n max)
          [{:error    :size-out-of-bounds
            :minimum  min
            :maximum  max
            :found    n}])))))

(defn- invalid-type [expected found]
  {:error    :invalid-type
   :expected expected
   :found    found})

(extend-type Class
  Validate
  (validate* [definition data]
    (if-not (instance? definition data)
      [(invalid-type definition (type data))])))

(defn- validate-seq [def data]
  (let [type-error (invalid-type def (mapv type data))]
    (loop [def def, data data, errors '()]
      (cond
       (empty? def)
       (if (seq data)
         (cons type-error errors)
         errors)

       (many? (first def))
       (if (valid? (.constraint (first def)) (first data))
         (recur def (rest data) errors)
         (recur (rest def) data errors))

       (optional? (first def))
       (if (valid? (.constraint (first def)) (first data))
         (recur (rest def) (rest data) errors)
         (recur (rest def) data errors))

       (empty? data)
       (cons type-error errors)

       :else
       (let [item-errors (validate* (first def) (first data))]
         (recur (rest def) (rest data) (concat errors item-errors)))))))

(extend-type clojure.lang.IPersistentVector
  Validate
  (validate* [definition data]
    (if (sequential? data)
      (validate-seq definition data)
      [{:error    :invalid-type
        :expected clojure.lang.Sequential
        :found    (type data)}])))

(defn- map-vals [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn- mandatory? [x]
  (not (or (many? x) (optional? x))))

(defn- valid-key? [def data]
  (if (mandatory? def)
    (valid? def data)
    (valid? (.constraint def) data)))

(defn- validate-map [def data]
  (let [type-error (invalid-type def (map-vals data type))]
    (letfn [(validate-map* [def data]
              (cond
               (and (empty? def) (not-empty data))
               [type-error]

               (and (empty? data) (some mandatory? (keys def)))
               [type-error]

               (not-empty data)
               (let [[dk dv] (first data)
                     data    (dissoc data dk)
                     matches (filter #(valid-key? (key %) dk) def)
                     errors  (for [[k v] matches]
                               (let [def (if (many? k) def (dissoc def k))]
                                 (concat (validate* v dv)
                                         (validate-map* def data))))]
                 (if (empty? matches)
                   [type-error]
                   (first (sort-by count errors))))))]
      (validate-map* def data))))

(extend-type clojure.lang.IPersistentMap
  Validate
  (validate* [definition data]
    (if (map? data)
      (validate-map definition data)
      [{:error    :invalid-type
        :expected clojure.lang.IPersistentMap
        :found    (type data)}])))

(extend-type java.util.regex.Pattern
  Validate
  (validate* [definition data]
    (cond
     (not (string? data))
     [(invalid-type String (type data))]
     (not (re-matches definition data))
     [{:error   :pattern-not-matching
       :pattern definition
       :found   data}])))

(defn- validate-literal [definition data]
  (if-not (= definition data)
    [{:error    :invalid-value
      :expected definition
      :found    data}]))

(extend-protocol Validate
  nil
  (validate* [def data] (validate-literal def data))
  Object
  (validate* [def data] (validate-literal def data)))
