(ns constraint.core)

(defprotocol Validate
  (validate* [definition data]))

(def messages
  {:invalid-type "data type does not match definition"
   :no-valid-constraint "no valid constraint in union"
   :count-differs "number of elements in data does not match definition"})

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

(extend-protocol Validate
  Class
  (validate* [definition data]
    (if-not (instance? definition data)
      [{:error    :invalid-type
        :expected definition
        :found    (type data)}]))
  nil
  (validate* [_ data]
    (if-not (nil? data)
      [{:error    :invalid-type
        :expected nil
        :found    (type data)}]))
  clojure.lang.IPersistentVector
  (validate* [definition data]
    (cond
     (not (sequential? data))
     [{:error    :invalid-type
       :expected clojure.lang.Sequential
       :found    (type data)}]
     (not= (count definition) (count data))
     [{:error    :count-differs
       :expected (count definition)
       :found    (count data)}]
     :else
     (seq (mapcat validate definition data)))))
