# constraint

[![Build Status](https://travis-ci.org/listora/constraint.png?branch=master)](https://travis-ci.org/listora/constraint)

A Clojure library for describing, validating and coercing user input.

**This library is still in early development, and has not yet been
  released.**

## Overview

Constraint draws upon ideas from Prismatic's [Schema][1] library and the
[core.typed][2] static type system. It's designed to manage user input,
particularly in the case of a public-facing web service. For this
reason, specific emphasis is placed on providing readable and useful
user-facing error messages.

Unlike Schema and core.typed, Constraint does not concern itself with
the data passed between internal functions. Constraint will not help
you find bugs in your application, but may help your users understand
what they're doing wrong.

Constraint can also turn a constraint definition into a valid
[JSON Schema][3] (WIP).

[1]: https://github.com/Prismatic/schema
[2]: https://github.com/clojure/core.typed
[3]: http://json-schema.org/

## Example

```clojure
{:name String
 :address [String]
 :birth-date Date
 :marital-status (U :single :married)
 (? :phone-number) (I String #"[0-9]+")}
```

## Syntax

Constraint works by describing the data it expects to find. The data
is considered valid if it matches a constraint definition.

#### Any

A constraint that always matches.

```clojure
(valid? Any "foobar")  ;; => true
(valid? Any :foobar)   ;; => true
```

#### Literals

A literal constraint can be a String, Keyword, Symbol or Number. It
matches if the data is exactly equal to the constraint.

```clojure
(valid? :foo :foo)  ;; => true
(valid? 5 6)        ;; => false
```

#### Types

A type constraint can be a class, interface or protocol. It matches if
the type of the data matches or satisfies the constraint.

```clojure
(valid? Double 1.5)  ;; => true
(valid? Number 1.5)  ;; => true
(valid? String 1.5)  ;; => false
```

#### Regular Expressions

A regular expression constraint matches if the data is a string, and
if the string matches the expression.

```clojure
(valid? #"a+" "aaaa")  ;; => true
```

#### Unions

A union is a way of combining constraints. It matches if *any* of the
inner constraints are valid.

```clojure
(valid? (U :yes :no) :yes)   ;; => true
(valid? (U String nil) nil)  ;; => true
```

#### Intersections

An intersection is another way of combining constraints. It matches if
*all* of the inner constraints are valid.

```clojure
(valid? (I String #"f..t") "foot")  ;; => true
```

#### Vectors

Constraints can be placed in a vector to match values contained in a
sequential collection. Each constraint in the vector is matched
against the corresponding item in the data collection. Constraints in
vectors can be arbitrarily nested.

```clojure
(valid? [Number Number] [4 5])      ;; => true
(valid? [String Number] ["foo" 5])  ;; => true
(valid? [Number] [1 2])             ;; => false
(valid? [String String] ["foo"])    ;; => false
```

#### Maps

Constraints can also be placed in maps to valid? associative
collections. Each key/value pair in the constraint map must
be matched against exactly one key/value pair in the data map.

```clojure
(valid? {:x Number} {:x 1})                          ;; => true
(valid? {String Number} {"x" 1}                      ;; => true
(valid? {String Number} {"x" 1, "y" 2})              ;; => false
(valid? {"x" Number, String Number} {"x" 1, "y" 2})  ;; => true
```

#### Many

In order to match many items of the same type within a collection, a
constraint may be marked with the `&` form to denote that it applies
zero or more times.

```clojure
(valid? [(& String)] ["foo" "bar" "baz"])              ;; => true
(valid? [(& String)] [])                               ;; => true
(valid? [String (& Number)] ["foo" 1 2 3])             ;; => true
(valid? [String (& Number) String] ["foo" 1 2 "bar"])  ;; => true
```

In a map, the many constraint must be placed on the key:

```clojure
(valid? {(& String) Number} {"x" 1, "y" 2})  ;; => true
(valid? {(& String) Number} {})              ;; => true
```

Outside of a collection, this form will cause a syntax exception.

#### Optional

To denote an optional key or value in a collection, a constraint may
be marked with the `?` form, to denote it is not required.

```clojure
(valid? [(? String) Number] [1])        ;; => true
(valid? [(? String) Number] ["foo" 1])  ;; => true
```

In maps, the optional constraint must be placed on the key:

```clojure
(valid? {(? :x) Number} {:x 10})  ;; => true
(valid? {(? :x) Number} {})       ;; => true
```

Outside of a collection, this form will cause a syntax exception.


## Validation

If you just want to know whether a constraint is valid or not, there
is the `valid?` function:

```clojure
(valid? String "foo")  ;; => true
```

But more often it's useful to get a list of error messages:

```clojure
(validate String 1)
=> ({:message  "data type does not match definition"
     :error    :invalid-type
     :expected java.lang.String
     :found    java.lang.Long})
```

Each error message will always contain an `:error` key that describes
the type of error.



## JSON Schema

Constraints can be serialized into [JSON Schema][1] for documentation
purposes.

[1]: http://json-schema.org/

For example:

```clojure
(json-schema {:name String, (? :age) Integer})
```

Will produce:

```json
{
    "$schema"   "http://json-schema.org/draft-04/schema#",
    "type"      "object",
    "required": ["name"],
    "additionalProperties": false,
    "properties": {
        "name": {"type": "string"},
        "age":  {"type": "integer"}
    }
}
```

Additional documentation can be included via the
`constraint.core/desc` function:

```clojure
(json-schema {:name (desc String "A person's full name")})
```

Produces:

```json
{
    "$schema"   "http://json-schema.org/draft-04/schema#",
    "type"      "object",
    "required": ["name"],
    "additionalProperties": false,
    "properties": {
        "name": {"type": "string", "doc": "A person's full name"},
    }
}
```

Because Constraint can validate more complex structures than JSON
Schema can, the resulting JSON Schema may not include all possible
validations, especially if custom validation types are used.


## License

Copyright © 2014 Listora

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
