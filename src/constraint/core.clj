(ns constraint.core
  "Core constraint types.")

(deftype AnyType [])

(def Any
  "A constraint that matches any data."
  (AnyType.))

(deftype Union [constraints])

(defn U
  "Create a union from one or more constraints. The resulting constraint is
  valid if any one of the containing constraints is valid."
  [& constraints]
  (Union. constraints))

(deftype Intersection [constraints])

(defn I
  "Create an intersection from one or more constraints. The resulting
  constraint is valid if and only if all the containing constraints are valid."
  [& constraints]
  (Intersection. constraints))

(deftype Optional [constraint])

(defn ?
  "Denote the inner constraint as optional when embedded in a collection."
  [constraint]
  (Optional. constraint))

(defmethod print-method Optional [^Optional opt ^java.io.Writer w]
  (.write w (str "(? " (pr-str (.constraint opt)) ")")))

(deftype Many [constraint])

(defn &
  "Denote the inner constraint as matching zero or more items in a collection."
  [constraint]
  (Many. constraint))

(defmethod print-method Many [^Many many ^java.io.Writer w]
  (.write w (str "(& " (pr-str (.constraint many)) ")")))

(deftype SizeBounds [min max])

(defn size
  "Create a size constraint. The constraint is valid if (count data) is within
  the bounds specified."
  ([max] (SizeBounds. 0 max))
  ([min max] (SizeBounds. min max)))
