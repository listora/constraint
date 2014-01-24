(ns constraint.validate
  "Validate a data structure against a constraint."
  (:require constraint.core
            [constraint.internal.parse :refer [split-vector]] ))

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
    (mapcat #(validate % data) (.constraints definition))))

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
   :found    (type found)})

(extend-type Class
  Validate
  (validate* [definition data]
    (if-not (instance? definition data)
      [(invalid-type definition data)])))

(extend-type clojure.lang.IPersistentVector
  Validate
  (validate* [definition data]
    (cond
     (some #(= '& %) definition)
     (let [[defs def-rest] (split-vector definition)]
       (concat
        (validate* (vec defs) (vec (take (count defs) data)))
        (mapcat #(validate* def-rest %) (drop (count defs) data))))
     (not (sequential? data))
     [{:error    :invalid-type
       :expected clojure.lang.Sequential
       :found    (type data)}]
     (not= (count definition) (count data))
     [{:error    :invalid-type
       :expected definition
       :found    (mapv type data)}]
     :else
     (seq (mapcat validate* definition data)))))

(extend-type clojure.lang.IPersistentMap
  Validate
  (validate* [definition data]
    (cond
     (not (map? data))
     [{:error    :invalid-type
       :expected clojure.lang.IPersistentMap
       :found    (type data)}]
     (not= (set (keys definition)) (set (keys data)))
     [{:error    :invalid-type
       :expected definition
       :found    (into {} (map (fn [[k v]] [k (type v)]) data))}]
     :else
     (seq (mapcat (fn [[k v]] (validate* v (data k))) definition)))))

(extend-type java.util.regex.Pattern
  Validate
  (validate* [definition data]
    (if (and (string? data) (not (re-matches definition data)))
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
