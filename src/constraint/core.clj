(ns constraint.core
  "Core constraint types.")

(deftype Union [constraints])

(defn U [& constraints]
  (Union. constraints))

(deftype Intersection [constraints])

(defn I [& constraints]
  (Intersection. constraints))

(deftype SizeBounds [min max])

(defn size
  ([max] (SizeBounds. 0 max))
  ([min max] (SizeBounds. min max)))
