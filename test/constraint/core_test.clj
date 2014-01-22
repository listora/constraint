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
             :found Double}])))
  (testing "nil"
    (is (empty? (validate nil nil)))
    (is (= (validate nil "foo")
           [{:error :invalid-type
             :message "data type does not match definition"
             :expected nil
             :found String}])))
  (testing "unions"
    (is (empty? (validate (U String nil) "foo")))
    (is (empty? (validate (U String nil) nil)))
    (is (not (empty? (validate (U String nil) 10)))))
  (testing "vectors"
    (is (empty? (validate [String Number] ["foo" 10]))))
  (testing "collection size"
    (is (= (validate [String Number] ["foo"])
           [{:error :count-differs
             :message "number of elements in data does not match definition"
             :expected 2
             :found 1}])))
  (testing "collection type"
    (is (= (validate [String Number] {"foo" 10})
           [{:error :invalid-type
             :message "data type does not match definition"
             :expected clojure.lang.Sequential
             :found clojure.lang.PersistentArrayMap}])))
  (testing "item types"
    (is (not (empty? (validate [String Number] ["foo" "10"]))))))
