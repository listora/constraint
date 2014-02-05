(ns constraint.core
  "Core constraint types.")

(defprotocol Constraint
  (constraint [x] "Return the constraint in the value x."))

(extend-protocol Constraint
  Object
  (constraint [x] x))

(defmethod print-method constraint.core.Constraint [c ^java.io.Writer w]
  (.write w (pr-str (constraint c))))

(deftype AnyType [])

(def Any
  "A constraint that matches any data."
  (AnyType.))

(deftype Description [inner doc]
  Constraint
  (constraint [x] inner))

(defn desc
  "Add a description to a constraint."
  [constraint doc-string]
  (Description. constraint doc-string))

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

(deftype Optional [inner]
  Constraint
  (constraint [x] inner))

(defn ?
  "Denote the inner constraint as optional when embedded in a collection."
  [constraint]
  (Optional. constraint))

(defmethod print-method Optional [^Optional opt ^java.io.Writer w]
  (.write w (str "(? " (pr-str (.constraint opt)) ")")))

(defn optional? [x]
  (instance? constraint.core.Optional x))

(deftype Many [inner]
  Constraint
  (constraint [x] inner))

(defn &
  "Denote the inner constraint as matching zero or more items in a collection."
  [constraint]
  (Many. constraint))

(defmethod print-method Many [^Many many ^java.io.Writer w]
  (.write w (str "(& " (pr-str (.constraint many)) ")")))

(defn many? [x]
  (instance? constraint.core.Many x))

(deftype SizeBounds [min max])

(defn size
  "Create a size constraint. The constraint is valid if (count data) is within
  the bounds specified."
  ([max] (SizeBounds. 0 max))
  ([min max] (SizeBounds. min max)))
