(ns constraint.coercions)

(defn- failed-coercion [type data]
  {:error    :failed-coercion
   :coercion type
   :found    data})

(defn make-coercion
  "Construct a coercion that maps data of one type to another type. Takes a
  :validate predicate to determine if the data can be coerced, and a :coerce
  function, which returns the coerced value."
  [[in-type out-type] & {:keys [validate coerce]}]
  (reify
    Transform
    (transform* [_ data]
      (let [results (transform* in-type data)]
        (if (seq (:errors results))
          results
          (if (validate data)
            {:value  (coerce data)}
            {:errors #{(failed-coercion out-type data)}}))))))

(def uuid-pattern
  #"[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")

(def string->uuid
  (make-coercion [String java.util.UUID]
    :validate #(re-matches uuid-pattern %)
    :coerce   #(java.util.UUID/fromString %)))

(def date-time-pattern
  #"^\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d([+-][0-2]\d:[0-5]\d|Z)$")

(def string->date
  (make-coercion [String java.util.Date]
    :validate #(re-matches date-time-pattern %)
    :coerce   #(.getTime (javax.xml.bind.DatatypeConverter/parseDateTime %)))) 

(def type-coercions
  "A set of standard type coercion rules for data parsed from JSON."
  {java.util.UUID string->uuid
   java.util.Date string->date})
