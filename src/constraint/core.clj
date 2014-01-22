(ns constraint.core)

(defprotocol Validate
  (validate [definition data]))

(def messages
  {:invalid-type "data type does not match definition"
   :no-valid-constraint "no valid constraint in union"})

(deftype Union [constraints]
  Validate
  (validate [_ data]
    (let [errors (map #(validate % data) constraints)]
      (if-not (some empty? errors)
        [{:error    :no-valid-constraint
          :message  (messages :no-valid-constraint)
          :failures (apply concat errors)}]))))

(defn U [& constraints]
  (Union. constraints))

(extend-protocol Validate
  Class
  (validate [definition data]
    (if-not (instance? definition data)
      [{:error    :invalid-type
        :message  (messages :invalid-type)
        :expected definition
        :found    (type data)}]))
  nil
  (validate [_ data]
    (if-not (nil? data)
      [{:error    :invalid-type
        :message  (messages :invalid-type)
        :expected nil
        :found    (type data)}])))
