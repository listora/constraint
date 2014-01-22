(ns constraint.core)

(defprotocol Validate
  (validate* [definition data]))

(def messages
  {:invalid-type "data type does not match definition"
   :invalid-value "data value does not match definition"
   :no-valid-constraint "no valid constraint in union"})

(defn validate [definition data]
  (for [error (validate* definition data)]
    (assoc error :message (messages (:error error)))))

(deftype Union [constraints]
  Validate
  (validate* [_ data]
    (let [errors (map #(validate % data) constraints)]
      (if-not (some empty? errors)
        [{:error    :no-valid-constraint
          :failures (apply concat errors)}]))))

(defn U [& constraints]
  (Union. constraints))

(deftype Intersection [constraints]
  Validate
  (validate* [_ data]
    (mapcat #(validate % data) constraints)))

(defn I [& constraints]
  (Intersection. constraints))

(defn- invalid-type [expected found]
  {:error    :invalid-type
   :expected expected
   :found    (type found)})

(extend-type Class
  Validate
  (validate* [definition data]
    (if-not (instance? definition data)
      [(invalid-type definition data)])))

(defn- split-vector [v]
  (let [[x [_ & y]] (split-with #(not= '& %) v)]
    (assert (= (count y) 1) "Only one item should be after & symbol")
    [(vec x) (first y)]))

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
     (seq (mapcat validate definition data)))))

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
