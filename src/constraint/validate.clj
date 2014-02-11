(ns constraint.validate
  "Validate a data structure against a constraint."
  (:require [constraint.core :refer (many? optional? constraint)]))

(defprotocol Validate
  (validate* [definition data]))

(def default-messages
  {:invalid-type "data type does not match definition"
   :invalid-value "data value does not match definition"
   :no-valid-constraint "no valid constraint in union"
   :size-out-of-bounds "data size is out of bounds"
   :pattern-not-matching "data does not match regular expression in definition"
   :failed-coercion "could not coerce data to expected format"
   :unexpected-keys "key(s) in data could not be matched to definition"
   :missing-keys "mandatory key(s) in definition could not be found in data"
   :unexpected-value "found additional values in list not in definition"
   :missing-value "unexpected end of list"})

(defn validate
  "Validate a data structure based on a constraint. If the data structure is
  valid, an empty collection is returned. If the data is invalid, a collection
  of errors is returned."
  [definition data]
  (for [error (:errors (validate* definition data))]
    (assoc error :message (default-messages (:error error)))))

(defn valid?
  "Return true if the data structure is valid according to the supplied
  constraint, or false if it is not."
  [definition data]
  (empty? (validate definition data)))

(defn consider [definition data]
  (merge {:value data :errors #{}}
         (validate* definition data)))

(extend-type constraint.core.AnyType
  Validate
  (validate* [_ data]))

(extend-type constraint.core.Description
  Validate
  (validate* [definition data] (validate* (constraint definition) data)))

(extend-type constraint.core.Union
  Validate
  (validate* [definition data]
    (let [results (map #(consider % data) (.constraints definition))
          match   (first (filter (comp empty? :errors) results))]
      (or match
          {:errors #{{:error    :no-valid-constraint
                      :failures (mapcat :error results)}}}))))

(extend-type constraint.core.Intersection
  Validate
  (validate* [definition data]
    (reduce
     (fn [{:keys [value errors]} definition]
       (let [result (consider definition value)]
         {:value (:value result)
          :errors (into errors (:errors result))}))
     {:value data, :errors #{}}
     (.constraints definition))))

(extend-type constraint.core.SizeBounds
  Validate
  (validate* [definition data]
    (let [min (.min definition)
          max (.max definition)]
      {:errors
       (if-let [n (try (count data) (catch Throwable _ nil))]
         (if-not (<= min n max)
           #{{:error    :size-out-of-bounds
              :minimum  min
              :maximum  max
              :found    n}}))})))

(defn- invalid-type [expected found]
  {:error    :invalid-type
   :expected expected
   :found    found})

(extend-type Class
  Validate
  (validate* [definition data]
    (if-not (instance? definition data)
      {:errors #{(invalid-type definition (type data))}})))

(defn- mandatory? [x]
  (not (or (many? x) (optional? x))))

(defn- add-key [error key]
  (update-in error [:keys] conj key))

(defn- unexpected-value [index value]
  {:keys  (list index)
   :error :unexpected-value
   :found value})

(defn- missing-value [missing]
  {:error   :missing-value
   :missing missing})

(defn- validate-seq [def data]
  (loop [def def, data data, value [] errors #{}]
    (let [def1  (first def)
          data1 (first data)
          index (count value)]
      (cond
       (empty? def)
       {:value  (into value data)
        :errors (if (seq data)
                 (conj errors (unexpected-value index data1))
                 errors)}

       (many? def1)
       (let [{e :errors v :value} (consider (constraint def1) data1)]
         (if (empty? e)
           (recur def (rest data) (conj value v) errors)
           (recur (rest def) data value errors)))

       (optional? def1)
       (let [{e :errors v :value} (consider (constraint def1) data1)]
         (if (empty? e)
           (recur (rest def) (rest data) (conj value v) errors)
           (recur (rest def) data value errors)))

       (empty? data)
       {:value  value
        :errors (cons (missing-value def1) errors)}

       :else
       (let [{e :errors v :value} (consider def1 data1)
             errors (into errors (map #(add-key % index) e))]
         (recur (rest def) (rest data) (conj value v) errors))))))

(extend-type clojure.lang.IPersistentVector
  Validate
  (validate* [definition data]
    (if (sequential? data)
      (validate-seq definition data)
      {:errors #{(invalid-type clojure.lang.Sequential (type data))}})))

(defn- validate-map [def data]
  (cond
   (and (empty? def) (not-empty data))
   {:errors #{{:error :unexpected-keys
               :found (set (keys data))}}}

   (and (empty? data) (some mandatory? (keys def)))
   {:errors #{{:error   :missing-keys
               :missing (set (filter mandatory? (keys def)))}}}

   (not-empty data)
   (let [[dk dv] (first data)
         data    (dissoc data dk)
         matches (for [[k v] def
                       :let  [{dk* :value, es :errors}
                              (consider (constraint k) dk)]
                       :when (empty? es)]
                   [k v dk*])]
     (if (empty? matches)
       {:errors #{{:error :unexpected-keys, :found [dk]}}}
       (let [results (for [[k v dk*] matches]
                       (let [def (if (many? k) def (dissoc def k))
                             {dv* :value, de :errors}      (consider v dv)
                             {data :value, errors :errors} (validate-map def data)]
                         {:value  (assoc data dk* dv*)
                          :errors (->> (map #(add-key % dk) de)
                                       (concat errors)
                                       (set))}))]
         (first (sort-by (comp count :errors) results)))))))

(extend-type clojure.lang.IPersistentMap
  Validate
  (validate* [definition data]
    (if (map? data)
      (validate-map definition data)
      {:errors #{(invalid-type clojure.lang.IPersistentMap (type data))}})))

(extend-type java.util.regex.Pattern
  Validate
  (validate* [definition data]
    (cond
     (not (string? data))
     {:errors #{(invalid-type String (type data))}}
     (not (re-matches definition data))
     {:errors #{{:error   :pattern-not-matching
                 :pattern definition
                 :found   data}}})))

(defn- validate-literal [definition data]
  (if-not (= definition data)
    {:errors
     #{{:error    :invalid-value
        :expected definition
        :found    data}}}))

(extend-protocol Validate
  nil
  (validate* [def data] (validate-literal def data))
  Object
  (validate* [def data] (validate-literal def data)))
