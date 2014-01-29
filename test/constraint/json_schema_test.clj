(ns constraint.json-schema-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.json-schema :refer :all]))

(deftest test-json-schema
  (testing "$schema key"
    (is (= (json-schema String)
           {"$schema" "http://json-schema.org/draft-04/schema#"
            "type" "string"})))

  (testing "basic types"
    (is (= (json-schema* String)  {"type" "string"}))
    (is (= (json-schema* Integer) {"type" "integer"}))
    (is (= (json-schema* Long)    {"type" "integer"}))
    (is (= (json-schema* Float)   {"type" "number"}))
    (is (= (json-schema* Double)  {"type" "number"}))
    (is (= (json-schema* Number)  {"type" "number"}))
    (is (= (json-schema* Boolean) {"type" "boolean"})))

  (testing "enums"
    (is (= (json-schema* (U :no :yes)) {"enum" ["no" "yes"]}))
    (is (= (json-schema* (U 1 2 3))    {"enum" [1 2 3]})))

  (testing "vectors"
    (is (= (json-schema* [String])
           {"type" "array", "items" [{"type" "string"}]}))
    (is (= (json-schema* [String Number])
           {"type" "array", "items" [{"type" "string"} {"type" "number"}]}))
    (is (= (json-schema* [(& String)])
           {"type" "array", "items" {"type" "string"}}))
    (is (= (json-schema* [String (& Number)])
           {"type" "array", "items" [{"type" "string"}]
            "additionalItems" {"type" "number"}})))

  (testing "basic schema"
    (is (= (json-schema {:foo String})
           {"$schema" "http://json-schema.org/draft-04/schema#"
            "type" "object"
            "properties" {"foo" {"type" "string"}}
            "required" ["foo"]
            "additionalProperties" false}))))
