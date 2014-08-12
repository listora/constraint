(ns constraint.core-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]))

(deftest test-union?
  (is (union? (U :foo :bar))))

(deftest test-intersection?
  (is (intersection? (I String "foo"))))

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

  (testing "descriptions"
    (is (empty? (validate (desc :x "something") :x)))
    (is (not (empty? (validate (desc :x "something") :y)))))

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
    (is (= (validate (U String nil) 10)
           [{:message "no valid constraint in union",
             :error :no-valid-constraint,
             :failures
             [{:message "data type does not match definition",
               :error :invalid-type,
               :expected java.lang.String,
               :found java.lang.Long}
              {:message "data value does not match definition",
               :error :invalid-value,
               :expected nil,
               :found 10}]}])))

  (testing "intersections"
    (is (empty? (validate (I Number Long) 10)))
    (is (not (empty? (validate (I Number Long) 10.0))))
    (is (not (empty? (validate (I Number Long) "10")))))

  (testing "vectors"
    (testing "valid"
      (is (empty? (validate [String Number] ["foo" 10]))))
    (testing "size"
      (is (= (validate [String Number] ["foo"])
             [{:error :missing-value
               :message "unexpected end of list"
               :missing Number}])))
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
      (is (not (empty? (validate [(? String) Number] ["foo" "bar" 3])))))
    (testing "error keys"
      (is (= (validate [String] ["foo" "bar"])
             [{:error :unexpected-value
               :keys [1]
               :message "found additional values in list not in definition"
               :found "bar"}]))
      (is (= (validate [String [Number String] String] ["a" ["b" "c"] "d"])
             [{:error :invalid-type
               :message "data type does not match definition"
               :keys [1 0]
               :expected Number
               :found String}]))))

  (testing "maps"
    (testing "valid"
      (is (empty? (validate {:foo String} {:foo "bar"})))
      (is (empty? (validate {:foo String, :bar Number} {:foo "x" :bar 1})))
      (is (empty? (validate {String Number} {"foo" 1}))))
    (testing "keys"
      (is (= (validate {:foo String} {:foo "bar" :baz "quz"})
             [{:error :unexpected-keys
               :message "key(s) in data could not be matched to definition"
               :found #{:baz}}])))
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
               :keys ["foo"]
               :expected Number
               :found String}])))
    (testing "value types"
      (is (= (validate {:foo String} {:foo 10})
             [{:error :invalid-type
               :message "data type does not match definition"
               :keys [:foo]
               :expected String
               :found Long}])))
    (testing "optional keys"
      (is (empty? (validate {(? :x) Number} {:x 1})))
      (is (empty? (validate {(? :x) Number} {})))
      (is (empty? (validate {:x Number (? :y) Number} {:x 1 :y 2})))
      (is (empty? (validate {:x Number (? :y) Number} {:x 1})))
      (is (not (empty? (validate {:x Number (? :y) Number} {})))))
    (testing "many keys"
      (is (empty? (validate {(& String) Number} {})))
      (is (empty? (validate {(& String) Number} {"foo" 1})))
      (is (empty? (validate {(& String) Number} {"foo" 1, "bar" 2})))
      (is (not (empty? (validate {(& String) Number} {"foo" 1, :bar 2}))))
      (is (empty? (validate {"foo" String, (& String) Number}
                            {"foo" "bar", "baz" 3})))
      (is (not (empty? (validate {"foo" String, (& String) Number}
                                 {"foo" 4, "baz" 5})))))
    (testing "error keys"
      (is (= (validate {:foo {:bar [String]}} {:foo {:bar [5]}})
             [{:error :invalid-type
               :message "data type does not match definition"
               :keys [:foo :bar 0]
               :expected String
               :found Long}])))))

(deftest test-valid?
  (is (true? (valid? String "foo")))
  (is (false? (valid? String 10))))

(deftest test-coercions
  (let [coercions {[String Long] (fn [x] {:value (Long/parseLong x)})}]
    (testing "valid?"
      (is (valid? Long "123" coercions))
      (is (valid? [Long] ["123"] coercions))
      (is (valid? {:foo Long} {:foo "123"} coercions)))

    (testing "coerce"
      (is (= (coerce Long "123" coercions) 123))
      (is (= (coerce [Long] ["123"] coercions) [123]))
      (is (= (coerce {:foo Long} {:foo "123"} coercions) {:foo 123})))))

(deftest test-transforms
  (let [coercions {[String Long] (fn [x] {:value (Long/parseLong x)})}
        schema {:foo Long}]
    (testing "valid"
      (let [r (transform schema {:foo "123"} coercions)]
        (is (= {:foo 123} (:value r)) "value is coerced")
        (is (empty? (:errors r)) "errors is empty")))
    (testing "invalid"
      (let [value {:bar "123"}
            transformed (transform schema value coercions)
            validated   (validate schema value)]
        (is (= value (:value transformed)) "value is preserved")
        (is (= validated (:errors transformed))
            "transform give same errors as validate")))))

(deftest test-custom-message
  (let [t (reify Transform
            (transform* [def data]
              {:errors #{{:error :invalid-type, :message "custom message"}}}))]
    (is (= [{:error :invalid-type, :message "custom message"}]
           (validate t 1)))))
