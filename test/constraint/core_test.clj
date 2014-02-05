(ns constraint.core-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]))

(deftest test-union?
  (is (union? (U :foo :bar))))

(deftest test-intersection?
  (is (intersection? (I String "foo"))))
