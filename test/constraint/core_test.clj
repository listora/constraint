(ns constraint.core-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]))

(deftest test-validate
  (testing "basic types"
    (is (empty? (validate String "foo"))))
  (testing "derived types"
    (is (empty? (validate Number 1)))
    (is (empty? (validate Number 1.2))))
  (testing "type error"
    (is (= (validate Integer 1.2)
           [{:error :invalid-type
             :message "data type does not match definition"
             :expected Integer
             :found Double}]))))
