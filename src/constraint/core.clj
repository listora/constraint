(ns constraint.core)

(defprotocol Validate
  (validate [definition data]))

(extend-protocol Validate
  Class
  (validate [definition data]
    (if-not (instance? definition data)
      [{:error    :invalid-type
        :message  "data type does not match definition"
        :expected definition
        :found    (type data)}])))
