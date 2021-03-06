# API Specification

## Interface

LHS: *Bind, Match*

RHS: *Set, Apply, Add, Remove*

### Bind

`Bind(^member {block} as <binding>)`

If the first string is attribute-like (starts with caret), bind from this attribute series.  Otherwise defaults
to `^type`.

The next string will be a block, substituted in the functions context.  $binding be set to the bound object's binding.

If block is followed by two strings in the form of `as <binding>`, then binding will be set to the passed binding.
Otherwise an autogenerated binding will be used, in the form of `<typeX>`.

### Match

`Match(args)`

Convert each arg string into the corresponding Soar test.  Basic form is `{^member test value as <binding>}`

Test must be one of the following:

* `=` must equal value (either a binding or constant value, such as a String)
* `>`, `<`, `>=`, `<=` must be greater than, less than, greater or equal to, less or equal to the numeric value
  or binding.
* `s>`, `s<`, `s>=`, `s<=` stable variants of the previous.  Cannot be bound.

Multiple `test value` pairs can be included in a single match call.

### Set

`Set(<binding> ^member value)`

Add one or more member to values with i-support.  If binding is missing, the production will lookup
a bound object with the same type.  Since this cannot be tracked at compile time, does not prevent or deny multiple
values for each member.

### Apply

`Apply(<binding> ^member value)`

Sets one or members to values with o-support, replacing the existing values if found.  If binding is missing, the
production will lookup a bound object with the same type.

### Add

`Add(<binding> ^member value)`

Adds one or more members with values using o-support, adding to the existing values.  If binding is missing, the
production will lookup a bound object with the same type.

### Remove

`Remove(<binding> ^member value)`

Removes one or more members with values using o-support.  If binding is missing, the production will lookup a bound
object with the same type.

## Input

LHS: *Bind, Match*

## Object

LHS: *Bind, Match*

RHS: *Construct, Initiate, Set, Apply, Add, Remove*

### Construct

`Construct(<parent> ^attribute ^member value as <binding>)`

Creates new object with o-support with multiple ^member value pairs.  If ^attribute is not set, assumed to be the type
name in lowercase.  If the parent binding is not known, assumed to be subobject.  Attempts to evaluate whether all
objects can be constructed in a single operator, as per NGS rules.  If `as <binding>` is missing, auto generates a
name in the form of `<typeX>`.

### Initiate

`Initiate(<parent> ^attribute ^member value as <binding>)`

Creates a new object with i-support.  Unlike construct, there are no restrictions on the number and structure of
objects created in a single production.

## Output

LHS: *Bind, Match*

RHS: *Construct, Initiate, Deply, Set, Apply, Add, Remove*

### Deploy

`Deploy(^attribute <binding>)`

Deep copies this binding to the output link.  If `^attribute` is not set, assumed to be the type name in lowercase.
Root object is always assumed to be the output link.

## Tags


