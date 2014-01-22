(ns constraint.core)

(defprotocol Validate
  (validate [definition data]))

(def messages
  {:invalid-type "data type does not match definition"})

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
