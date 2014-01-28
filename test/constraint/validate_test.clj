(ns constraint.validate-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.validate :refer :all]))

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

  (testing "any type"
    (testing "valid"
      (is (empty? (validate Any "foo")))
      (is (empty? (validate Any 10)))
      (is (empty? (validate Any nil)))))
  
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

  (testing "regexes"
    (testing "matches"
      (is (empty? (validate #"fo+" "foooo"))))
    (testing "doesn't match"
      (let [re #"fo+"]
        (is (= (validate re "foa")
               [{:error :pattern-not-matching
                 :message "data does not match regular expression in definition"
                 :pattern re
                 :found "foa"}]))))
    (testing "implicit string type"
      (is (= (validate #"foo" 10)
             [{:error :invalid-type
               :message "data type does not match definition"
               :expected String
               :found Long}]))))
  
  (testing "unions"
    (is (empty? (validate (U String nil) "foo")))
    (is (empty? (validate (U String nil) nil)))
    (is (not (empty? (validate (U String nil) 10)))))

  (testing "intersections"
    (is (empty? (validate (I Number Long) 10)))
    (is (not (empty? (validate (I Number Long) 10.0))))
    (is (not (empty? (validate (I Number Long) "10")))))

  (testing "sizes"
    (testing "valid"
      (is (empty? (validate (size 5) [])))
      (is (empty? (validate (size 5) [1 2 3 4 5])))
      (is (empty? (validate (size 3 5) [1 2 3 4]))))
    (testing "errors"
      (is (not (empty? (validate (size 5) [1 2 3 4 5 6]))))
      (is (not (empty? (validate (size 3 5) [1 2]))))
      (is (= (validate (size 2 3) [1 2 3 4])
             [{:error :size-out-of-bounds
               :message "data size is out of bounds"
               :minimum 2
               :maximum 3
               :found 4}]))))
  
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
    (testing "inner constraints"
      (is (not (empty? (validate [String Number] ["foo" "10"])))))
    (testing "many constraint"
      (is (empty? (validate [(& String)] ["foo" "bar" "baz"])))
      (is (empty? (validate [(& String)] [])))
      (is (empty? (validate [Number (& String)] [10 "foo" "bar"])))
      (is (not (empty? (validate [Number (& String)] []))))
      (is (not (empty? (validate [Number (& String)] ["foo"]))))
      (is (not (empty? (validate [Number (& String)] [10 "foo" 5]))))
      (is (empty? (validate [(& String) Number] ["foo" "bar" 5])))
      (is (empty? (validate [(& String) Number] [7])))
      (is (not (empty? (validate [(& String) Number] ["foo"])))))
    (testing "optional constraint"
      (is (empty? (validate [(? String) Number] ["foo" 10])))
      (is (empty? (validate [(? String) Number] [10])))
      (is (not (empty? (validate [(? String) Number] []))))
      (is (not (empty? (validate [(? String) Number] ["foo"]))))
      (is (not (empty? (validate [(? String) Number] ["foo" "bar" 3]))))))

  (testing "maps"
    (testing "valid"
      (is (empty? (validate {:foo String} {:foo "bar"})))
      (is (empty? (validate {:foo String, :bar Number} {:foo "x" :bar 1})))
      (is (empty? (validate {String Number} {"foo" 1}))))
    (testing "keys"
      (is (= (validate {:foo String} {:foo "bar" :baz "quz"})
             [{:error :invalid-type
               :message "data type does not match definition"
               :expected {:foo String}
               :found {:foo String :baz String}}])))
    (testing "type"
      (is (= (validate {:foo String} [:foo "bar"])
             [{:error :invalid-type
               :message "data type does not match definition"
               :expected clojure.lang.IPersistentMap
               :found clojure.lang.PersistentVector}])))
    (testing "generic type"
      (is (= (validate {String Number} {"foo" "bar"})
             [{:error :invalid-type
               :message "data type does not match definition"
               :expected Number
               :found String}])))
    (testing "value types"
      (is (= (validate {:foo String} {:foo 10})
             [{:error :invalid-type
               :message "data type does not match definition"
               :expected String
               :found Long}])))))

(deftest test-valid?
  (is (true? (valid? String "foo")))
  (is (false? (valid? String 10))))
