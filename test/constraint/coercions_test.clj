(ns constraint.coercions-test
  (:import [java.util Date UUID])
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.coercions :refer :all]))

(deftest test-json-coercions
  (testing "UUID"
    (let [uuid #uuid "129ef863-b19e-44fc-bc64-c4d6f3a425ae"]
      (is (valid? UUID (str uuid) json-coercions))
      (is (= (coerce UUID (str uuid) json-coercions) uuid))))
  
  (testing "Date"
    (let [date "2014-02-03T21:07:25Z"]
      (is (valid? Date date json-coercions))
      (is (= (coerce Date date json-coercions) #inst "2014-02-03T21:07:25Z")))))
