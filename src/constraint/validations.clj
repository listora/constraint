(ns constraint.validations
  (:require [constraint.core :refer [Transform]]))

(defn- below-minimum [minimum found]
  {:error   :below-minimum
   :minimum minimum
   :found   found})

(defn- above-maximum [maximum found]
  {:error   :above-maximum
   :maximum maximum
   :found   found})

(defn- size-too-small [minimum found]
  {:error   :size-too-small
   :minimum minimum
   :found   found})

(defn- size-too-large [maximum found]
  {:error   :size-too-large
   :maximum maximum
   :found   found})

(defn- try-count [x]
  (try (count x) (catch Throwable _ nil)))

(deftype Minimum [min]
  Transform
  (transform* [_ data]
    (if (and (number? data) (< data min))
      {:errors #{(below-minimum min data)}})))

(deftype Maximum [max]
  Transform
  (transform* [_ data]
    (if (and (number? data) (> data max))
      {:errors #{(above-maximum max data)}})))

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

(defn minimum
  "Create a minimum numerical constraint. The constraint is valid if the data is
  equal to or above the supplied minimum."
  [min]
  (Minimum. min))

(defn maximum
  "Create a maximum numerical constraint. The constraint is valid if the data is
  equal to or above the supplied maximum."
  [min]
  (Maximum. min))

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
