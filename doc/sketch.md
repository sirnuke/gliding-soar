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
    members {
        channel-id: String
        service-id: String
        server-id: String
        metadata: Metadata
    }
    tags {
        private: Boolean
    }
    matches {
        is-private: Subst {
            [ngs-is-tagged $binding $::Channel::tags::private $::NGS_YES]
        }
        is-public: Subst {
            [ngs-is-tagged $binding $::Channel::tags::private $::NGS_NO]
        }
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
