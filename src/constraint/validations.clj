(ns constraint.validations
  (:require [constraint.core :refer [Transform]]))

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

(deftype MinSize [min]
  Transform
  (transform* [_ data]
    (if-let [n (try-count data)]
      (if (< n min)
        {:errors #{(size-too-small min n)}}))))

(deftype MaxSize [max]
  Transform
  (transform* [_ data]
    (if-let [n (try-count data)]
      (if (> n max)
        {:errors #{(size-too-large max n)}}))))

(defn min-size
  "Create a minimum size constraint. The constraint is valid if (count data) is
  equal or over the supplied minimum."
  [min]
  (MinSize. min))

(defn max-size
  "Create a maximum size constraint. The constraint is valid if (count data) is
  equal or under the supplied maximum."
  [max]
  (MaxSize. max))
