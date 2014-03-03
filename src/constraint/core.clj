(ns constraint.core
  "Define, validate and coerce constraints definitions."
  (:require [constraint.i18n :as i18n]))

(deftype AnyType [])

(def Any
  "A constraint that matches any data."
  (AnyType.))

(deftype Description [constraint doc])

(defn desc
  "Add a description to a constraint."
  [constraint doc-string]
  (Description. constraint doc-string))

(defmethod print-method Description [^Description desc ^java.io.Writer w]
  (.write w (pr-str (.constraint desc))))

(deftype Union [constraints])

(defn U
  "Create a union from one or more constraints. The resulting constraint is
  valid if any one of the containing constraints is valid."
  [& constraints]
  (Union. constraints))

(defn union? [x]
  (instance? constraint.core.Union x))

(deftype Intersection [constraints])

(defn I
  "Create an intersection from one or more constraints. The resulting
  constraint is valid if and only if all the containing constraints are valid."
  [& constraints]
  (Intersection. constraints))

(defn intersection? [x]
  (instance? constraint.core.Intersection x))

(deftype Optional [constraint])

(defn ?
  "Denote the inner constraint as optional when embedded in a collection."
  [constraint]
  (Optional. constraint))

(defmethod print-method Optional [^Optional opt ^java.io.Writer w]
  (.write w (str "(? " (pr-str (.constraint opt)) ")")))

(defn optional? [x]
  (instance? constraint.core.Optional x))

(deftype Many [constraint])

(defn &
  "Denote the inner constraint as matching zero or more items in a collection."
  [constraint]
  (Many. constraint))

(defmethod print-method Many [^Many many ^java.io.Writer w]
  (.write w (str "(& " (pr-str (.constraint many)) ")")))

(defn many? [x]
  (instance? constraint.core.Many x))

(defprotocol Transform
  (transform* [definition data]))

(def ^:dynamic *coercions* {})

(defn- find-coercion [def data]
  (let [coercion-type [(type data) def]]
    (some->> *coercions* (filter #(isa? (key %) coercion-type)) first val)))

(defn transform
  "Transform a data structure according to a definition. Returns a map with the
  keys :value, containing the transformed data, and :errors, containing a set
  of validation errors. Takes an optional map of coercions that maps a pair of
  types to a coercion function."
  [definition data & [{:as coercions}]]
  (binding [*coercions* (merge *coercions* coercions)]
    (merge {:value data :errors #{}} (transform* definition data))))

(defn- default-message [error]
  (get-in i18n/default-messages [:en (:error error)]))

(defn validate
  "Validate a data structure against a definition. Returns a set of validation
  errors. The data is valid if the set is empty. Takes an optional map of
  coercions (see transform)."
  [definition data & [{:as coercions}]]
  (for [error (:errors (transform definition data coercions))]
    (if (:message error)
      error
      (assoc error :message (default-message error)))))

(defn valid?
  "Validate a data structure against a definition. Returns true if the data is
  valid, false otherwise. Takes an optional map of coercions."
  [definition data & [{:as coercions}]]
  (empty? (validate definition data coercions)))

(defn coerce
  "Transform a data structure according to a definition. Throws an exception if
  the data is not valid. Takes an optional map of coercions."
  [definition data & [{:as coercions}]]
  (let [{:keys [value errors]} (transform definition data coercions)]
    (assert (empty? errors))
    value))

(defn- no-valid-constraint [results]
  {:error    :no-valid-constraint
   :failures (mapcat :error results)})

(defn- invalid-type [expected found]
  {:error    :invalid-type
   :expected expected
   :found    found})

(defn- invalid-value [expected found]
  {:error    :invalid-value
   :expected expected
   :found    found})

(defn- unexpected-value [index value]
  {:keys  (list index)
   :error :unexpected-value
   :found value})

(defn- missing-value [missing]
  {:error   :missing-value
   :missing missing})

(defn- unexpected-keys [keys]
  {:error :unexpected-keys
   :found (set keys)})

(defn- missing-keys [keys]
  {:error   :missing-keys
   :missing (set keys)})

(extend-protocol Transform
  AnyType
  (transform* [_ data])
  
  Description
  (transform* [definition data] (transform* (.constraint definition) data))
  
  Union
  (transform* [definition data]
    (let [results (map #(transform % data) (.constraints definition))
          match   (first (filter (comp empty? :errors) results))]
      (or match
          {:errors #{(no-valid-constraint results)}})))
  
  Intersection
  (transform* [definition data]
    (reduce
     (fn [{:keys [value errors]} definition]
       (let [result (transform definition value)]
         {:value (:value result)
          :errors (into errors (:errors result))}))
     {:value data, :errors #{}}
     (.constraints definition)))

  Class
  (transform* [definition data]
    (if-let [coercion (find-coercion definition data)]
      (coercion data)
      (if-not (instance? definition data)
        {:errors #{(invalid-type definition (type data))}})))

  java.util.regex.Pattern
  (transform* [definition data]
    (cond
     (not (string? data))
     {:errors #{(invalid-type String (type data))}}
     (not (re-matches definition data))
     {:errors #{{:error   :pattern-not-matching
                 :pattern definition
                 :found   data}}}))

  Object
  (transform* [definition data]
    (if-not (= definition data)
      {:errors #{(invalid-value definition data)}}))

  nil
  (transform* [definition data]
    (if-not (= definition data)
      {:errors #{(invalid-value definition data)}})))

(defn- mandatory? [x]
  (not (or (many? x) (optional? x))))

(defn- add-key [error key]
  (update-in error [:keys] conj key))

(defn- transform-seq [def data]
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
       (let [{e :errors v :value} (transform (.constraint def1) data1)]
         (if (empty? e)
           (recur def (rest data) (conj value v) errors)
           (recur (rest def) data value errors)))

       (optional? def1)
       (let [{e :errors v :value} (transform (.constraint def1) data1)]
         (if (empty? e)
           (recur (rest def) (rest data) (conj value v) errors)
           (recur (rest def) data value errors)))

       (empty? data)
       {:value  value
        :errors (cons (missing-value def1) errors)}

       :else
       (let [{e :errors v :value} (transform def1 data1)
             errors (into errors (map #(add-key % index) e))]
         (recur (rest def) (rest data) (conj value v) errors))))))

(extend-type clojure.lang.IPersistentVector
  Transform
  (transform* [definition data]
    (if (sequential? data)
      (transform-seq definition data)
      {:errors #{(invalid-type clojure.lang.Sequential (type data))}})))

(defn- transform-key [def data]
  (if (mandatory? def)
    (transform def data)
    (transform (.constraint def) data)))

(defn- transform-map [def data]
  (cond
   (and (empty? def) (not-empty data))
   {:errors #{(unexpected-keys (keys data))}}

   (and (empty? data) (some mandatory? (keys def)))
   {:errors #{(missing-keys (filter mandatory? (keys def)))}}

   (not-empty data)
   (let [[dk dv] (first data)
         data    (dissoc data dk)
         matches (for [[k v] def
                       :let  [{dk* :value, es :errors} (transform-key k dk)]
                       :when (empty? es)]
                   [k v dk*])]
     (if (empty? matches)
       {:errors #{{:error :unexpected-keys, :found [dk]}}}
       (let [results (for [[k v dk*] matches]
                       (let [def (if (many? k) def (dissoc def k))
                             {dv* :value, de :errors}      (transform v dv)
                             {data :value, errors :errors} (transform-map def data)]
                         {:value  (assoc data dk* dv*)
                          :errors (->> (map #(add-key % dk) de)
                                       (concat errors)
                                       (set))}))]
         (first (sort-by (comp count :errors) results)))))))

(extend-type clojure.lang.IPersistentMap
  Transform
  (transform* [definition data]
    (if (map? data)
      (transform-map definition data)
      {:errors #{(invalid-type clojure.lang.IPersistentMap (type data))}})))
