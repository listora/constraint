# constraint

A Clojure library for describing, validating and coercing user input.

**This library is still in early development, and has not yet been
  released.**

## Overview

Constraint draws upon ideas from Prismatic's [Schema][1] library and the
[core.typed][2] static type system. It's designed to manage user input,
particularly in the case of a public-facing web service. For this
reason, specific emphasis is placed on providing readable and useful
error messages.

Unlike Schema and core.typed, Constraint does not concern itself with
the data passed between internal functions. Constraint will not help
you find bugs in your application, but may help your users understand
what they're doing wrong.

Constraint can also turn a constraint definition into a valid
[JSON Schema][3] (WIP).

[1]: https://github.com/Prismatic/schema
[2]: https://github.com/clojure/core.typed
[3]: http://json-schema.org/

## Syntax

Constraint works by describing the data it expects to find. The
simplest form of constraint is to match the type:

```clojure
(validate String "foo")
(validate Number 100)
```

Constraint also understands types embedded in vectors or maps:

```clojure
(validate [Number Number] [16 45])
(validate [String '& Number] ["foo" 1 2 3])
(validate {:name String} {:name "Bob Jones"})
```

As well as types, literal values such as strings, numbers or keywords
can be used:

```clojure
(validate :foo :foo)
(validate nil nil)
```

For more complex validations, Constraint provides *unions* and
*intersections*. A union is considered valid if any **one** containing
constraint is valid. 

```clojure
(validate (U String nil) "foo")
(validate (U String nil) nil)
```

Nullable types are one common use-case for unions. Another is enums:

```clojure
(validate (U :yes :no) :yes)
```

An intersection is only valid if **all** containing constraints are
valid. This can be used to combine additional constraints.

Regular expressions can be used to further constrain String types:

```clojure
(validate (I String #"fo+") "foooo")
```

And the `size` function can be used to set a limit on the maximum and
minimum size of a countable collection or string:

```clojure
(validate (I String (size 16)) "foobar")
```

Further validations can be constructed by implementing the
`constraint.validate/Validate` protocol.


## License

Copyright © 2014 Listora

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
