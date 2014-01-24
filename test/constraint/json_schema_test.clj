(ns constraint.json-schema-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.json-schema :refer :all]))

(deftest test-json-schema
  (testing "basic schema"
    (is (= (json-schema {:foo String})
           {"type" "object"
            "properties" {"foo" {"type" "string"}}
            "additionalProperties" false}))))
