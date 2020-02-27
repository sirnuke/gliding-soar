# Manually opens a LHS context, with root_binding as the root binding
# TODO: This probably be either a state or a goal
proc lhs { root_binding block } {
    set binding $root_binding

    ::Glide::_claim_binding {} lhs lhs $root_binding root

    variable ::Glide::_in_hs 1
    set ret [subst $block]
    variable ::Glide::_in_hs 0

    ::Glide::_reset_lhs

    return $ret
}

# Indicates 
proc operator { args } {
    variable ::Glide::_first_operator_action 1

    variable ::Glide::_in_operator 1
    set ret [subst $block]
    variable ::Glide::_in_operator 0

    return $ret
}


namespace eval Glide {
    namespace export set-scope bind-input bind-output _bind match _set _apply _add _remove _construct _initiate _deploy _reset_lhs

    # Tracking variables
    variable _in_lhs 0
    variable _in_operator 0
    variable _first_operator_action
    variable _scope {}

    array set _bindings {}
    array set _anonymous_bindings {}

    # Sets the current scope variable
    #
    # All Glide productions, plus logging, between set-scope calls will be
    # labeled as belonging to this common scope.  A possible, but not
    # required, strategy would be to use the relative path and filename of the
    # current file.
    #
    # Yes, ideally this would be set automagically, but JTCL is awful and
    # doesn't support [info frame], so scripts can't progmatically determine
    # where they are.  Call your congressional representatives and complain.
    #
    # If you elect to use this functionality, highly recommend setting it for
    # all files.  Since there's no way for Glide to know which file it is,
    # this variable will carryover into new files, if not set.
    #
    # args  List of tokens to combine into the scope
    proc set-scope { args } {
        variable ::Glide::_scope [join $args *]
    }

    proc bind-input { args } {
        _assert_lhs bind-input
        if { [llength $arguments] == 0 } {
            set binding "<input-link>"
        } elseif { [llength $arguments] == 2 } {
            set binding [lindex $arguments 1]
        } else {
            _dump_error "<root>" bind-input lhs "usage: (as <binding>)?"
        }
        _claim_binding "" "bind-input" lhs $binding "input-link"
        return "([state] ^io.input-link $binding)"
    }

    proc bind-output { args } {
        _assert_lhs bind-output
        if { [llength $arguments] == 0 } {
            set binding "<output-link>"
        } elseif { [llength $arguments] == 2 } {
            set binding [lindex $arguments 1]
        } else {
            _dump_error "<root>" bind-input lhs "usage: (as <binding>)?"
        }
        _claim_binding "" "bind-output" lhs $binding "output-link"
        return "([state] ^io.output-link $binding)"
    }

    proc _bind { type_ arguments } {
        _assert_lhs "${type_}::bind"
        set type $type_
        upvar binding parent_binding
        if { [llength $arguments] < 1 || [llength $arguments] > 4 } {
            _dump_error $type bind lhs "usage: ^member? { block } (as <binding>)?"
        }
        set binding {}
        set attribute {}
        if { [llength $arguments] == 1 } {
            set block [lindex $arguments 0]
        } elseif { [llength $arguments] == 2 } {
            set attribute [lindex $arguments 0]
            set block [lindex $arguments 1]
        } elseif { [llength $arguments] == 3 } {
            set block [lindex $arguments 0]
            set binding [lindex $arguments 2]
        } else {
            set attribute [lindex $arguments 0]
            set block [lindex $arguments 1]
            set binding [lindex $arguments 3]
        }

        if { $binding eq "" } {
            set binding [_next_anonymous_binding $type]
        }

        _claim_binding $type bind lhs $binding $type

        if { $attribute eq "" } {
            set attribute [_default_attribute $type]
        }

        return "($parent_binding $attribute $binding)[subst $block]"
    }

    proc match { args } {
        upvar binding binding
        upvar type type
        if { [llength $args] < 1 } {
            _dump_error $type match lhs "Must have at least one test"
        }
        set ret "($binding"
        foreach test $args {
            if { [llength $test] < 1 } {
                _dump_error $type match lhs "Each test must have at least one element (the member to test)"
            }
            set member [lindex $test 0]
            set as_binding ""
            set checks [list]
            for { set idx 1 } { $idx < [llength $test] } { } {
                set test_type [lindex $test $idx]
                incr idx

                if { $found_binding ne ""} {
                    _dump_error $type match lhs "Test $test_type on $member after 'as <binding>' clause!"
                }

                if { $idx >= [llength $test] } {
                    _dump_error $type match lhs "Test $test_type on $member missing value"
                }

                set test_value [lindex $test $idx]
                incr idx
                switch $test_type {
                    "as" { set as_binding $test_value }
                    "="  { lappend checks $test_type $test_value }
                    ">"  { lappend checks $test_type $test_value }
                    "<"  { lappend checks $test_type $test_value }
                    "<=" { lappend checks $test_type $test_value }
                    ">=" { lappend checks $test_type $test_value }
                }
            }

            if { $as_binding ne "" } {
                # TODO: claim the binding
            }
            if { [llength $checks] == 0 } {
                set ret "$ret $member $as_binding" 
            } else {
                set ret "$ret $member {$as_binding $checks} "
            }
        }

        return "$ret)"
    }

    proc _set { type arguments } {
        if { [llength $arguments] < 2 } {
            _dump_error $type set rhs "usage: <binding>? (^member value>)+"
        }
        set idx 0
        if { [expr [llength $arguments] % 2] == 1 } {
            incr idx
            set binding [lindex $arguments 0]
        } else {
            set binding [_lookup_default_binding $type]
        }
        set ret "($binding"
        while { $idx < [llength $arguments] } {
            set member [lindex $arguments $idx]
            incr idx
            set value [lindex $arguments $idx]
            incr idx
            set ret "$ret $member $value"
        }
        return "$ret)"
    }

    # TODO: These three bad bois are basically the same
    # TODO: Would also be nice to have them inside a wrapping operator function of some sort (sets binding and whatnot)
    proc _apply { type arguments } {
        if { [llength $arguments] < 2 } {
            _dump_error $type apply rhs "usage: <binding>? (^member value>)+"
        }
        variable _first_operator_action
        set idx 0
        if { [expr [llength $arguments] % 2] == 1 } {
            incr idx
            set binding [lindex $arguments 0]
        } else {
            set binding [_lookup_default_binding $type]
        }
        set ret {}
        while { $idx < [llength $arguments] } {
            set member [_soar_attribute_to_ngs [lindex $arguments $idx]]
            incr idx
            set value [lindex $arguments $idx]
            incr idx
            if $_first_operator_action {
                variable _first_operator_action 0
                set ret "$ret[ngs-create-attribute-by-operator <s> $binding $member $value $::NGS_REPLACE_IF_EXISTS]"
            } else {
                set ret "$ret[ngs-add-primitive-side-effect $::NGS_SIDE_EFFECT_ADD $binding $member $value $::NGS_REPLACE_IF_EXISTS]"
            }
        }
        return $ret
    }

    proc _add { type arguments } {
        if { [llength $arguments] < 2 } {
            _dump_error $type add rhs "usage: <binding>? (^member value>)+"
        }
        variable _first_operator_action
        set idx 0
        if { [expr [llength $arguments] % 2] == 1 } {
            incr idx
            set binding [lindex $arguments 0]
        } else {
            set binding [_lookup_default_binding $type]
        }
        set ret {}
        while { $idx < [llength $arguments] } {
            set member [_soar_attribute_to_ngs [lindex $arguments $idx]]
            incr idx
            set value [lindex $arguments $idx]
            incr idx
            if $_first_operator_action {
                variable _first_operator_action 0
                set ret "$ret[ngs-create-attribute-by-operator <s> $binding $member $value $::NGS_ADD_TO_SET]"
            } else {
                set ret "$ret[ngs-add-primitive-side-effect $::NGS_SIDE_EFFECT_ADD $binding $member $value $::NGS_ADD_TO_SET]"
            }
        }
        return $ret
    }

    proc _remove { type arguments } {
        if { [llength $arguments] < 2 } {
            _dump_error $type remove rhs "usage: <binding>? (^member value>)+"
        }
        variable _first_operator_action
        set idx 0
        if { [expr [llength $arguments] % 2] == 1 } {
            incr idx
            set binding [lindex $arguments 0]
        } else {
            set binding [_lookup_default_binding $type]
        }
        set ret {}
        while { $idx < [llength $arguments] } {
            set member [_soar_attribute_to_ngs [lindex $arguments $idx]]
            incr idx
            set value [lindex $arguments $idx]
            incr idx
            if $_first_operator_action {
                variable _first_operator_action 0
                set ret "$ret[ngs-remove-attribute-by-operator <s> $binding $member $value]"
            } else {
                set ret "$ret[ngs-add-primitive-side-effect $::NGS_SIDE_EFFECT_REMOVE $binding $member $value]"
            }
        }
        return $ret
    }

    proc _construct { type arguments } {
        if { [llength $arguments] < 2 } {
            _dump_error $type construct rhs "usage: <parent> ^attribute (^member value)+ (as <binding>)?"
        }
        variable _first_operator_action
        set binding {}
        set parent {}
        set idx 0
        set parent [lindex $arguments 0]
        incr idx
        set attribute [_soar_attribute_to_ngs [lindex $arguments $idx]]
        incr idx
        set attrs [list]
        while { $idx < [llength $arguments] } {
            set member [lindex $arguments $idx]
            incr idx
            if { $member eq "as" } {
                set binding [lindex $arguments $idx]
                incr idx
                if { $idx < [llength $arguments] } {
                    _dump_error $type construct rhs "as <binding> must be the final entry"
                }
            } else {
                lappend attrs [_soar_attribute_to_ngs $member] [lindex $arguments $idx]
                incr idx
            }
        }

        if { $binding eq "" } {
            set binding [_next_anonymous_binding $type]
        }

        if $_first_operator_action {
            variable _first_operator_action 0
            return [ngs-create-typed-object-by-operator "<s>" $parent $attribute [set ${type}::type] $binding $attrs $::NGS_REPLACE_IF_EXISTS]
        } else {
            return [ngs-create-typed-sub-object-by-operator $parent $attribute [set ${type}::type] $binding $attrs $::NGS_REPLACE_IF_EXISTS]
        }
    }

    proc _construct_set { type arguments } {
        if { [llength $arguments] < 2 } {
            _dump_error $type construct_set rhs "usage: <parent> ^attribute (^member value)+ (as <binding>)?"
        }
        variable _first_operator_action
        set binding {}
        set idx 0
        set parent [lindex $arguments 0]
        incr idx
        set attribute [_soar_attribute_to_ngs [lindex $arguments $idx]]
        incr idx
        set attrs [list]
        while { $idx < [llength $arguments] } {
            set member [lindex $arguments $idx]
            incr idx
            if { $member eq "as" } {
                set binding [lindex $arguments $idx]
                incr idx
                if { $idx < [llength $arguments] } {
                    _dump_error $type construct_set rhs "as <binding> must be the final entry"
                }
            } else {
                lappend attrs [_soar_attribute_to_ngs $member] [lindex $arguments $idx]
                incr idx
            }
        }

        if { $binding eq "" } {
            set binding [_next_anonymous_binding $type]
        }

        if $_first_operator_action {
            variable _first_operator_action 0
            return [ngs-create-typed-object-by-operator "<s>" $parent $attribute [set ${type}::type] $binding $attrs $::NGS_ADD_TO_SET]
        } else {
            return [ngs-create-typed-sub-object-by-operator $parent $attribute [set ${type}::type] $binding $attrs $::NGS_ADD_TO_SET]
        }
    }

    proc _initiate { type arguments } {
        if { [llength $arguments] < 2 } {
            _dump_error $type initiate rhs "usage: <parent> ^attribute (^member value)+ (as <binding>)?"
        }

        set binding {}
        set idx 0
        set parent [lindex $arguments 0]
        incr idx
        set attribute [_soar_attribute_to_ngs [lindex $arguments $idx]]
        incr idx
        set attrs [list]
        while { $idx < [llength $arguments] } {
            set member [lindex $arguments $idx]
            incr idx
            if { $member eq "as" } {
                set binding [lindex $arguments $idx]
                incr idx
                if { $idx < [llength $arguments] } {
                    _dump_error $type construct_set rhs "as <binding> must be the final entry"
                }
            } else {
                lappend attrs [_soar_attribute_to_ngs $member] [lindex $arguments $idx]
                incr idx
            }
        }

        return [ngs-create-typed-object $parent $attribute [set ${type}::type] $binding $attrs]
    }

    proc _deploy { type arguments } {
        if { [llength $arguments] != 2 } {
            _dump_error $type deploy rhs "usage: ^attribute <binding>"
        }
        variable _first_operator_action
        set attribute [_soar_attribute_to_ngs [lindex $arguments 0]]
        set binding [lindex $arguments 1]

        if $_first_operator_action {
            variable _first_operator_action 0
            return [ngs-deep-copy-by-operator "<s>" "<input-link>" $attribute $binding $::NGS_ADD_TO_SET]
        } else {
            _dump_error $type deploy rhs "deploy must be the first action for this operator (and preferably only)"
        }
    }

    proc _is_multiple { type member } {
        if { [dict get ${[set ${type}::members]} multiple] eq "true" } {
            return 1
        } else {
            return 0
        }
    }

    proc _check_member { type function member value tag add creation support multi_add } {
        if { [array names [set ${type}::members] -exact $member] eq "" } {
            _dump_error $type $function rhs "no member $member defined"
        }
        set definition ${[set ${type}::members]($member)}

        # Confirm support is
        if { $support eq "i" && [dict get $definition support] eq "o" } {
            _dump_error $type $function rhs "cannot apply i-support modification $value to o-support member $member"
        } elseif { $support eq "o" && [dict get $definition support] eq "i" } {
            _dump_error $type $function rhs "cannot apply o-support modification $value to i-support member $member"
        }

        # Confirm tag matches expected
        if { [dict get $definition tag] eq "true" && !$tag } {
            _dump_error $type $function rhs "member $member is a tag"
        } elseif { [dict get $definition tag] eq "false" && $tag } {
            _dump_error $type $function rhs "member $member is not a tag"
        }

        # TODO: Compare the type of value with the member definition

        # Confirm if const, this modification is being done at the object's creation
        if { [dict get $definition const] eq "true" && !$creation } {
            _dump_error $type $function rhs "cannot modify const member $member to $value outside the object's creation"
        }

        # If doing a multi add ($NGS_ADD_TO_SET), check whether member allows multiples
        # Note that we can't really check this for i-support modifications, nor whether there's actually multiple members
        if { [dict get $definition multiple] eq "false" && $multi_add } {
            _dump_error $type $function rhs "cannot add value $value as a set member to member $member"
        }
    }

    proc _dump_error { type func side message } {
        set scope ""
        if { $::Glide::_scope ne "" } {
            set scope "$scope$::Glide::_scope "
        }
        error "${scope}Bad $side $type.$func call: $message"
    }

    proc _claim_binding { type func side binding binding_type } {
        set existing [array names _binding -exact $binding]
        if { $existing ne "" } {
            _dump_error $type $func $side "Cannot set binding $binding as $binding_type already exists as $existing"
        }
        set _bindings($binding) $binding_type
    }

    proc _next_anonymous_binding { type } {
        set identifier [set ${type}::identifier]
        if { [array names _anonymous_bindings -exact $identifier] -ne "" } {
            set identifier "$identifier${_anonymous_bindings($identifier)}"
            incr _anonymous_bindings($identifier)
        } else {
            set _anonymous_bindings($identifier) 1
        }

        set binding "<$identifier>"

        return $binding
    }

    proc _lookup_default_binding { type } {
        # TODO: Confirm actually bound and whatnot
        return "<[set ${type}::identifier]>"
    }

    proc _default_attribute { type } {
        return "^[set ${type}::identifier]"
    }

    proc _soar_attribute_to_ngs { attribute } {
        return [string trimleft $attribute "^"]
    }

    proc _state { } {
        # TODO: Lookup if the user has it set to something else
        return "<s>"
    }

    proc _goal { } {
        # TODO: Lookup the correct value
        return "<g>"
    }

    proc _assert_lhs { func } {
        if { !$_in_lhs } {
            _dump_error "" $func lhs "must be within a LHS block"
        }
    }

    proc _reset_lhs { } {
        array set _bindings {}
        array set _anonymous_bindings
    }
}