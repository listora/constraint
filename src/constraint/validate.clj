(ns constraint.validate
  "Validate a data structure against a constraint."
  (:require [constraint.core :refer (many? optional? constraint)]))

(defprotocol Validate
  (validate* [definition data]))

(defprotocol WalkData
  (walk-data* [definition f data]))

(def messages
  {:invalid-type "data type does not match definition"
   :invalid-value "data value does not match definition"
   :no-valid-constraint "no valid constraint in union"
   :size-out-of-bounds "data size is out of bounds"
   :pattern-not-matching "data does not match regular expression in definition"
   :failed-coercion "could not coerce data to expected format"})

(defn walk-data
  "Performs a depth-first, post-order traversal of a data structure matched
  with a constraint definition. Takes a function, f, that expects a constraint
  and data structure, and returns a data structure."
  [f definition data]
  (walk-data* definition f data))

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
  (validate* [definition data] (validate* (constraint definition) data)))

(extend-type constraint.core.Union
  Validate
  (validate* [definition data]
    (let [errors (map #(validate % data) (.constraints definition))]
      (if-not (some empty? errors)
        [{:error    :no-valid-constraint
          :failures (apply concat errors)}])))
  WalkData
  (walk-data* [definition f data]
    (let [def (first (filter #(valid? % data) (.constraints definition)))]
      (f definition (walk-data* def f data)))))

(extend-type constraint.core.Intersection
  Validate
  (validate* [definition data]
    (vec (set (mapcat #(validate % data) (.constraints definition)))))
  WalkData
  (walk-data* [definition f data]
    (f definition (reduce #(walk-data* %2 f %1) data (.constraints definition)))))

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

(defn- walk-seq [def data]
  (let [type-error (invalid-type def (mapv type data))]
    (loop [def def, data data, pairs [], errors '()]
      (let [def1 (first def), data1 (first data)]
        (cond
         (empty? def)
         [pairs (if (seq data) (cons type-error errors) errors)]

         (many? (first def))
         (if (valid? (constraint def1) data1)
           (recur def (rest data) (conj pairs [(constraint def1) data1]) errors)
           (recur (rest def) data pairs errors))

         (optional? (first def))
         (if (valid? (constraint def1) data1)
           (recur (rest def) (rest data) (conj pairs [(constraint def1) data1]) errors)
           (recur (rest def) data pairs errors))

         (empty? data)
         [pairs (cons type-error errors)]

         :else
         (let [errors (concat errors (validate* def1 data1))
               pairs  (conj pairs [def1 data1])]
           (recur (rest def) (rest data) pairs errors)))))))

(extend-type clojure.lang.IPersistentVector
  Validate
  (validate* [definition data]
    (if (sequential? data)
      (second (walk-seq definition data))
      [{:error    :invalid-type
        :expected clojure.lang.Sequential
        :found    (type data)}]))
  WalkData
  (walk-data* [definition f data]
    (if (sequential? data)
      (let [pairs (first (walk-seq definition data))]
        (f definition (mapv (fn [[def data]] (walk-data* def f data)) pairs)))
      data)))

(defn- map-vals [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn- mandatory? [x]
  (not (or (many? x) (optional? x))))

(defn- walk-map [def data]
  (let [type-error (invalid-type def (map-vals data type))]
    (letfn [(walk-map* [def data]
              (cond
               (and (empty? def) (not-empty data))
               [nil [type-error]]

               (and (empty? data) (some mandatory? (keys def)))
               [nil [type-error]]

               (not-empty data)
               (let [[dk dv] (first data)
                     data    (dissoc data dk)
                     matches (filter #(valid? (constraint (key %)) dk) def)
                     results (for [[k v] matches]
                               (let [definition     (if (many? k) def (dissoc def k))
                                     [pairs errors] (walk-map* definition data)]
                                 [(cons [[(constraint k) v] [dk dv]] pairs)
                                  (concat (validate* v dv) errors)]))]
                 (if (empty? matches)
                   [nil [type-error]]
                   (first (sort-by (comp count second) results))))))]
      (walk-map* def data))))

(extend-type clojure.lang.IPersistentMap
  Validate
  (validate* [definition data]
    (if (map? data)
      (second (walk-map definition data))
      [{:error    :invalid-type
        :expected clojure.lang.IPersistentMap
        :found    (type data)}]))
  WalkData
  (walk-data* [definition f data]
    (if (map? data)
      (let [pairs (first (walk-map definition data))]
        (->> (for [[[k v] [dk dv]] pairs]
               (f [k v] [(walk-data* k f dk) (walk-data* v f dv)]))
             (into {})
             (f definition)))
      data)))

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

(extend-protocol WalkData
  nil
  (walk-data* [definition f data] (f definition data))
  Object
  (walk-data* [definition f data] (f definition data)))
