(ns constraint.internal.parse
  "Functions for parsing data structures.")

(defn split-vector [v]
  (let [[x [_ & y]] (split-with #(not= '& %) v)]
    (assert (<= (count y) 1) "Only one item should be after & symbol")
    [(vec x) (first y)]))
