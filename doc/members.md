# Members

## Flags

### One vs Many

WMEs are usually only one, but NGS very explicitly supports one vs multiple WMEs with the same attribute.

Blocks:

### Constant

Makes sense to have constant WMEs, essentially anything that must be set when the object is created and should never be modified.

Blocks: Operator or elaboration modifiable

### Optional

Attributes that can be set on creation, but are not required.

Blocks: Required

### Required

Attributes that must be set on creation, but for o-support objects can be modified in an o manner.

Blocks: optional, elaboration modifiable unless Many

### Operator modifiable

Existing o-support attributes that can be modified by an operator.

Blocks: constant, elaboration modifiable

Technically you wouldn't want operator modifiable if in an i-supported object, but possible can't predict that.

### Elaboration modifiable

Attributes that can be added using an i-support production.

Blocks: constant, operator modifiable, required unless Many


