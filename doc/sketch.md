# Goal

```tcl
sp "$SCOPE*elaborate*fresh*message
    [ngs-match-goal <s> $G_MAINTAIN_F_CHAIN <g>]
    [lhs <s> {
        [bind-channel {
            [bind-behaviors {
                [ngs-bind $binding f-chain:<f>.last]
            }]
            [bind-message {
                [message-is-not-self]
                [bind-parse { [parse-is-category f]}]
                [ngs-stable-gte $binding earliest-clock <last>]
            }]
        }]
    }]
-->
    [ngs-tag <message-1> $FRESH_TAG]"

prod $SCOPE*elaborate*fresh*message when {
    [::Channel::bind {
        [::Behaviors::bind {
            [ngs-bind $binding f-chain:<f>.last]
        }]
        [::Message::bind {
            [::Message::is-not-self]
            [::Parse::bind {
                [::Parse::is-category f]
                [ngs-stable-gte $binding earliest-clock <last>]
            }]
        }]
    }]
} then {
    [::Message::tag-fresh]
}
```

```json
  {
      "name": "Channel",
      "type": "input",
      "members": {
          "channel-id": "String",
          "service-id": "String",
          "server-id": "String",
          "name": "String",
          "metadata": "Metadata"
      },
      "procs": {
          "is-private": "[ngs-is-tagged $binding private $::NGS_YES]"
      }
  }
```

```kotlin
struct("Channel", type=Input) {
    member("channel-id", "String")
    member("service-id", "String")
    member("server-id", "String")
    member("metadata", "Metadata")
    tag("private", "Boolean")
    lhs("is-private") {
        subst("[ngs-is-tagged $binding $::Channel::tags::private $::NGS_YES]")
    }
}
```

```glide
input Channel {
    parameters {
        channel-id: String
        service-id: String
        server-id: String
        metadata: Metadata
    }
    members {
        i tag private: Boolean
        o tag other: Tag
    }
    matches {
        is-private: Subst
            << [ngs-is-tagged $binding $::Channel::tags::private $::NGS_YES] >>
        is-public: Subst
            << [ngs-is-tagged $binding $::Channel::tags::private $::NGS_NO] >>
    }
}
```

```glide
interface ChannelLocation {
    members {
        channel-id: String
        service-id: String
        server-id: String
    }
}

object Response: ChannelLocation {
    members {
        message: String
        voice?: Boolean
    }
}
```

```glide
input Channel {
    parameters {
        channel-id: String
        service-id: String
        metadata: Metadata
    }

    elaborables {
        tag private: Boolean
    }

    operables {
        tag closed: Tag
    }

    matches {
        is-private: Subst << >>
    }
}
```

```glide
input Channel {
    param channel-id: String
    param service-id: String
    param metadata: Metadata

    i tag private: Boolean
    o tag closed: Tag

    subst is-private << >>
}
```

```glide
output Message: ChannelLocation {
    param message: String
}
```

```tcl

namespace eval Message {
    namespace export output output-by-operator

    proc output { params } {
        if params has binding {
            set binding params['binding']
        } else if parent object output-link {
            upvar binding binding
        } else if output binding set {
            set binding production::output_binding
        } else {
            error "unable to resolve output binding"
        }
        set message params['message']
        set channel-id params['channel-id']
        set server-id params['server-id']
        set service-id params['service-id']

        return [ngs-create-typed-object-by]
    }

    proc output-by-operator { params } {
        if params has binding {
            set binding params['binding']
        } else if parent object output-link {
            upvar binding binding
        } else if output binding set {
            set binding production::output_binding
        } else {
            error "unable to resolve output binding"
        }
        set message params['message']
        set channel-id params['channel-id']
        set server-id params['server-id']
        set service-id params['service-id']

        if params has operator_binding {
            set operator_binding params['operator_binding']
            if production::has_operator[$operator_binding] {
                error "Operator already set!"
            }
        } else {
            if production::has_operator['default'] {
                error "Default operator already set!"
            }
            set operator_binding ngs_default
        }

        return [ngs-create-typed-object-by-operator ...]
    }
}

```
