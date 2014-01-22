(ns constraint.core-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]))

(deftest test-validate
  (testing "basic types"
    (testing "valid"
      (is (empty? (validate String "foo"))))
    (testing "derived"
      (is (empty? (validate Number 1)))
      (is (empty? (validate Number 1.2))))
    (testing "error"
      (is (= (validate Integer 1.2)
             [{:error :invalid-type
               :message "data type does not match definition"
               :expected Integer
               :found Double}]))))
  
  (testing "values"
    (testing "strings"
      (is (empty? (validate "foo" "foo")))
      (is (= (-> (validate "foo" "bar") first :error) :invalid-value)))
    (testing "numbers"
      (is (empty? (validate 5 5)))
      (is (= (-> (validate 5 6) first :error) :invalid-value)))
    (testing "keywords"
      (is (empty? (validate :foo :foo)))
      (is (= (-> (validate :foo :bar) first :error) :invalid-value)))
    (testing "nil"
      (is (empty? (validate nil nil)))
      (is (= (validate nil "foo")
             [{:error :invalid-value
               :message "data value does not match definition"
               :expected nil
               :found "foo"}]))))
  
  (testing "unions"
    (is (empty? (validate (U String nil) "foo")))
    (is (empty? (validate (U String nil) nil)))
    (is (not (empty? (validate (U String nil) 10)))))
  
  (testing "vectors"
    (testing "valid"
      (is (empty? (validate [String Number] ["foo" 10]))))
    (testing "size"
      (is (= (validate [String Number] ["foo"])
             [{:error :invalid-type
               :message "data type does not match definition"
               :expected [String Number]
               :found [String]}])))
    (testing "type"
      (is (= (validate [String Number] {"foo" 10})
             [{:error :invalid-type
               :message "data type does not match definition"
               :expected clojure.lang.Sequential
               :found clojure.lang.PersistentArrayMap}])))
    (testing "item types"
      (is (not (empty? (validate [String Number] ["foo" "10"])))))
    (testing "rest type"
      (is (empty? (validate ['& String] ["foo" "bar" "baz"])))
      (is (empty? (validate ['& String] [])))
      (is (empty? (validate [Number '& String] [10 "foo" "bar"])))
      (is (not (empty? (validate [Number '& String] []))))
      (is (not (empty? (validate [Number '& String] ["foo"]))))
      (is (not (empty? (validate [Number '& String] [10 "foo" 5])))))))
