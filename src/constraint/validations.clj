(ns constraint.validations
  (:require [constraint.core :refer [Transform]]))

(defn- validation [f]
  (reify Transform (transform* [_ data] {:errors (f data)})))

(defn- try-count [x]
  (try (count x) (catch Throwable _ nil)))

(defn- size-too-small [minimum found]
  {:error   :size-too-small
   :minimum minimum
   :found   found})

(defn- size-too-large [maximum found]
  {:error   :size-too-large
   :maximum maximum
   :found   found})

(defn min-size
  "Create a minimum size constraint. The constraint is valid if (count data) is
  equal or over the supplied minimum."
  [min]
  (validation
   (fn [data]
     (if-let [n (try-count data)]
       (if (< n min)
         #{(size-too-small min n)})))))

(defn max-size
  "Create a maximum size constraint. The constraint is valid if (count data) is
  equal or under the supplied maximum."
  [max]
  (validation
   (fn [data]
     (if-let [n (try-count data)]
       (if (> n max)
         #{(size-too-large max n)})))))
