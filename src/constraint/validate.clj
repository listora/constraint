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

(defn valid?
  "Return true if the data structure is valid according to the supplied
  constraint, or false if it is not."
  [definition data]
  (empty? (validate* definition data)))

(extend-type constraint.core.AnyType
  Validate
  (validate* [_ _] '()))

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

(extend-type constraint.core.Many
  Validate
  (validate* [definition data]
    (validate* (.constraint definition) data)))

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

(defn- many? [x]
  (instance? constraint.core.Many x))

(defn- validate-seq [def data]
  (let [type-error (invalid-type def (mapv type data))]
    (loop [def def, data data, errors '()]
      (cond
       (empty? def)
       (if (seq data)
         (cons type-error errors)
         errors)

       (many? (first def))
       (if (valid? (first def) (first data))
         (recur def (rest data) errors)
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

(defn- match-keys [definition data-key]
  (->> (keys definition)
       (filter #(valid? % data-key))
       (seq)))

(defn- validate-kv [definition [dk dv]]
  (let [errors (->> (match-keys definition dk)
                    (map #(validate* (definition %) dv)))]
    (if-not (some empty? errors)
      (if (> (count errors) 1)
        [{:error    :no-valid-constraint
          :failures (apply concat errors)}]
        (first errors)))))

(extend-type clojure.lang.IPersistentMap
  Validate
  (validate* [definition data]
    (cond
     (not (map? data))
     [{:error    :invalid-type
       :expected clojure.lang.IPersistentMap
       :found    (type data)}]
     (not (every? #(match-keys definition %) (keys data)))
     [{:error    :invalid-type
       :expected definition
       :found    (into {} (map (fn [[k v]] [k (type v)]) data))}]
     :else
     (seq (mapcat #(validate-kv definition %) data)))))

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
