(ns constraint.validations-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.validations :refer :all]))

(deftest test-minimum
  (is (valid? (minimum 0) 0))
  (is (valid? (minimum 0) 2))
  (is (not (valid? (minimum 0) -1)))
  (is (= (validate (minimum 1) 0)
         [{:error :below-minimum
           :message "numerical data is below minimum definition"
           :minimum 1
           :found 0}])))

(deftest test-maximum
  (is (valid? (maximum 3) 1))
  (is (valid? (maximum 3) 3))
  (is (not (valid? (maximum 3) 4)))
  (is (= (validate (maximum 3) 5)
         [{:error :above-maximum
           :message "numerical data exceeds maximum definition"
           :maximum 3
           :found 5}])))

(deftest test-min-size
  (is (valid? (min-size 1) [1]))
  (is (valid? (min-size 1) [1 2]))
  (is (not (valid? (min-size 1) [])))
  (is (= (validate (min-size 1) [])
         [{:error :size-too-small
           :message "size of data below minimum definition"
           :minimum 1
           :found 0}])))

(deftest test-max-size
  (is (valid? (max-size 3) [1]))
  (is (valid? (max-size 3) [1 2 3]))
  (is (not (valid? (max-size 2) [1 2 3])))
  (is (= (validate (max-size 2) [1 2 3])
         [{:error :size-too-large
           :message "size of data exceeds maximum definition"
           :maximum 2
           :found 3}])))
