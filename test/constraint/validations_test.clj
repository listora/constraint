(ns constraint.validations-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.validations :refer :all]))

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
