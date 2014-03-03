(ns constraint.i18n)

(def default-messages
  {:en
   {:invalid-type "data type does not match definition"
    :invalid-value "data value does not match definition"
    :no-valid-constraint "no valid constraint in union"
    :pattern-not-matching "data does not match regular expression in definition"
    :failed-coercion "could not coerce data to expected format"
    :unexpected-keys "key(s) in data could not be matched to definition"
    :missing-keys "mandatory key(s) in definition could not be found in data"
    :unexpected-value "found additional values in list not in definition"
    :missing-value "unexpected end of list"
    :size-too-small "size of data below minimum definition"
    :size-too-large "size of data exceeds maximum definition"}})
