(ns constraint.coerce-test
  (:import [java.util Date UUID])
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.coerce :refer :all]
            [constraint.validate :refer (valid?)]))

(deftest test-validate-coercions
  (testing "UUID type"
    (let [uuid "129ef863-b19e-44fc-bc64-c4d6f3a425ae"]
      (is (not (valid? UUID uuid)))
      (is (valid? (transform UUID json-rules) uuid))))
  
  (testing "Date type"
    (let [date "2014-02-03T21:07:25Z"]
      (is (not (valid? Date date)))
      (is (valid? (transform Date json-rules) date)))))

(deftest test-coerce
  (testing "UUID type"
    (is (= (coerce (transform UUID json-rules) "129ef863-b19e-44fc-bc64-c4d6f3a425ae")
           #uuid "129ef863-b19e-44fc-bc64-c4d6f3a425ae")))
  
  (testing "Date type"
    (is (= (coerce (transform Date json-rules) "2014-02-03T21:07:25Z")
           #inst "2014-02-03T21:07:25Z")))

  (testing "other types"
    (is (= (coerce (transform String json-rules) "foobar") "foobar")))
  
  (testing "inside maps"
    (is (= (coerce (transform {:timestamp Date} json-rules)
                   {:timestamp "2014-02-03T21:07:25Z"})
           {:timestamp #inst "2014-02-03T21:07:25Z"})))

  (testing "inside vectors"
    (is (= (coerce (transform [UUID] json-rules)
                   ["129ef863-b19e-44fc-bc64-c4d6f3a425ae"])
           [#uuid "129ef863-b19e-44fc-bc64-c4d6f3a425ae"]))))
