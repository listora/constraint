(ns constraint.json-schema-test
  (:require [clojure.test :refer :all]
            [constraint.core :refer :all]
            [constraint.validations :refer :all]
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

  (testing "any"
    (is (= (json-schema* Any) {})))

  (testing "descriptions"
    (is (= (json-schema* (desc String "Username"))
           {"type" "string", "doc" "Username"}))
    (is (= (json-schema* {:name (-> String (desc "User's name"))})
           {"type" "object"
            "required" ["name"]
            "additionalProperties" false
            "properties" {"name" {"type" "string", "doc" "User's name"}}})))

  (testing "patterns"
    (is (= (json-schema* #"a+") {"type" "string", "pattern" "a+"})))

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

  (testing "maps"
    (is (= (json-schema* {:foo String})
           {"type" "object"
            "properties" {"foo" {"type" "string"}}
            "required" ["foo"]
            "additionalProperties" false}))
    (is (= (json-schema* {(? :foo) String})
           {"type" "object"
            "properties" {"foo" {"type" "string"}}
            "additionalProperties" false}))
    (is (= (json-schema* {(& String) Number})
           {"type" "object"
            "properties" {}
            "additionalProperties" {"type" "number"}})))

  (testing "intersections"
    (is (= (json-schema* (I String #"a+"))
           {"type" "string", "pattern" "a+"}))
    (is (= (json-schema* (I #"^a" #"z$"))
           {"allOf" [{"type" "string", "pattern" "^a"} {"pattern" "z$"}]})))

  (testing "unions"
    (is (= (json-schema* (U String Number))
           {"oneOf" [{"type" "string"} {"type" "number"}]})))

  (testing "enums"
    (is (= (json-schema* (U :no :yes)) {"enum" ["no" "yes"]}))
    (is (= (json-schema* (U 1 2 3))    {"enum" [1 2 3]})))

  (testing "size bounds"
    (is (= (json-schema* (I String (max-size 5)))
           {"type" "string", "maxLength" 5}))
    (is (= (json-schema* (I String (min-size 1)))
           {"type" "string", "minLength" 1}))
    (is (= (json-schema* (I [(& Any)] (max-size 3)))
           {"type" "array", "items" {}, "maxItems" 3}))
    (is (= (json-schema* (I [(& Any)] (min-size 1)))
           {"type" "array", "items" {}, "minItems" 1})))

  (testing "minimum and maximum"
    (is (= (json-schema* (I Integer (minimum 0)))
           {"type" "integer", "minimum" 0}))
    (is (= (json-schema* (I Number (minimum 1.0)))
           {"type" "number", "minimum" 1.0}))
    (is (= (json-schema* (I Integer (maximum 3)))
           {"type" "integer", "maximum" 3}))
    (is (= (json-schema* (I Number (maximum 4.0)))
           {"type" "number", "maximum" 4.0}))))
