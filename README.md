# constraint

[![Build Status](https://travis-ci.org/listora/constraint.png?branch=master)](https://travis-ci.org/listora/constraint)

A Clojure library for describing, validating and coercing user input.

**This library is still in very early development.**

#### Leiningen dependency

```clojure
[listora/constraint "0.0.3"]
```


## Introduction

Constraint draws upon ideas from Prismatic's [Schema][1] library and the
[core.typed][2] static type system. It's designed to manage user input,
particularly in the case of a public-facing web service. For this
reason, specific emphasis is placed on providing readable and useful
user-facing error messages.

Unlike Schema and core.typed, Constraint does not concern itself with
the data passed between internal functions. Constraint will not help
you find bugs in your application, but may help your users understand
what they're doing wrong.

[1]: https://github.com/Prismatic/schema
[2]: https://github.com/clojure/core.typed


## Overview

Constraint uses a concise but expressive syntax for describing data.
This definition is known as a *constraint*.

```clojure
(def Product
  {:id       UUID
   :name     String
   :price    (I BigDecimal (minimum 0))
   :stock    (I Integer (minimum 0))
   :tags     #{(& Keyword)}
   (? :size) [Number Number Number]})
```

The syntax borrows elements from core.typed and Clojure destructuring
syntax. A full definition of the syntax can be found [in the wiki][3].

[3]: https://github.com/listora/constraint/wiki/Syntax


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
