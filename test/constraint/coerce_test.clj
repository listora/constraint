(ns constraint.coerce-test
  (:import [java.util Date UUID])
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.coerce :refer :all]
            [constraint.coerce.json :as json]
            [constraint.validate :refer (valid?)]))

(deftest test-validate-coercions
  (testing "UUID type"
    (let [uuid "129ef863-b19e-44fc-bc64-c4d6f3a425ae"]
      (is (not (valid? UUID uuid)))
      (is (valid? (transform UUID json/type-coercions) uuid))))
  
  (testing "Date type"
    (let [date "2014-02-03T21:07:25Z"]
      (is (not (valid? Date date)))
      (is (valid? (transform Date json/type-coercions) date)))))

(deftest test-coerce
  (testing "UUID type"
    (is (= (coerce (transform UUID json/type-coercions)
                   "129ef863-b19e-44fc-bc64-c4d6f3a425ae")
           #uuid "129ef863-b19e-44fc-bc64-c4d6f3a425ae")))
  
  (testing "Date type"
    (is (= (coerce (transform Date json/type-coercions) "2014-02-03T21:07:25Z")
           #inst "2014-02-03T21:07:25Z")))

  (testing "other types"
    (is (= (coerce (transform String json/type-coercions) "foobar") "foobar")))
  
  (testing "inside maps"
    (is (= (coerce (transform {:timestamp Date} json/type-coercions)
                   {:timestamp "2014-02-03T21:07:25Z"})
           {:timestamp #inst "2014-02-03T21:07:25Z"})))

  (testing "inside vectors"
    (is (= (coerce (transform [UUID] json/type-coercions)
                   ["129ef863-b19e-44fc-bc64-c4d6f3a425ae"])
           [#uuid "129ef863-b19e-44fc-bc64-c4d6f3a425ae"]))))
