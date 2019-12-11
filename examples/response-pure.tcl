namespace eval Response {

    namespace export create
    proc create { service-id server-id channel-id message { cooldown "" } { destination "" } { attribute "response" } { binding "<response>" }} {
        ::glide::check-rhs
        ::glide::check-string ${service-id}
        # ...

        set binding_ [::glide::claim-binding $binding Response]
        if { $destination eq "" } {
            set destination_ [::glide::output-binding]
        } else {
            set destination_ $destination
        }

        ::glide::check-i-attribute $destination_ $attribute "Response"

        set ret "
            [ngs-create-attribute $destination_ $attribute $binding_]
            [ngs-create-attribute $binding service-id ${service-id}]
            [ngs-create-attribute $binding server-id ${server-id}]
            [ngs-create-attribute $binding channel-id ${channel-id}]
            [ngs-create-attribute $binding message ${message}]
        "

        if { $cooldown ne "" } {
            ::glide::check-integer ${cooldown}
            set ret "$ret
                [ngs-create-attribute $binding cooldown ${cooldown}]"
        }

        return $ret
    }

    namespace export create-by-operator
    proc create-by-operator { service-id server-id channel-id message { cooldown "" } { destination "" } { attribute "response" } { binding "<response>" } { state "" } { add_prefs "=" } } {
        ::glide::check-rhs
        ::glide::check-operator
        ::glide::check-string ${service-id}
        # ...
        ::glide::check-prefs $add_prefs

        set binding_ [::glide::claim-binding $binding Response]
        if { $state eq "" } {
            set state_ [::glide::state-binding]
        } else {
            set state_ $state
        }
        if { $destination eq "" } {
            set destination_ [::glide::output-binding]
        } else {
            set destination_ $destination
        }

        ::glide::check-o-attribute $destination_ $attribute "Response"

        set attrs "service-id {|${service-id}|} server-id {|${server-id}|} channel-id {|${channel-id}|} message {|${message}|}"

        if { ${cooldown} ne "" } {
            ::glide::check-integer ${cooldown}
            set attrs "$attrs cooldown ${cooldown}"
        }

        return "[ngs-create-typed-object-by-operator $state_ $destination_ $attribute Response $binding_ $attrs $::NGS_ADD_TO_SET $add_prefs]"
    }
    
}