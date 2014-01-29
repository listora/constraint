(ns constraint.json-schema-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.json-schema :refer :all]))

(deftest test-json-schema
  (testing "basic schema"
    (is (= (json-schema {:foo String})
           {"$schema" "http://json-schema.org/draft-04/schema#"
            "type" "object"
            "properties" {"foo" {"type" "string"}}
            "required" ["foo"]
            "additionalProperties" false}))))
