# Manually opens a LHS context, with root_binding as the root binding
# TODO: This probably be either a state or a goal
proc lhs { root_binding block } {
    set binding $root_binding

    ::Glide::_claim_binding {} lhs lhs $root_binding root

    variable ::Glide::_in_hs 1
    set ret [subst $block]

    ::Glide::_reset_lhs

    return $ret
}

# Indicates 
proc operator { block { log "" } { add_prefs "=" } } {
    variable ::Glide::_first_operator_action 1
    variable ::Glide::_in_operator 1
    variable ::Glide::_operator_preferences $add_prefs

    set ret [subst $block]

    ::Glide::_reset_operator

    if { $logs ne "" } {
        # TODO: Add logging side effect
    }

    return $ret
}

namespace eval Glide {
    # Tracking variables
    variable _in_lhs 0
    variable _in_operator 0
    variable _first_operator_action
    variable _operator_preferences
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
    namespace export set-scope

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
        return "([_state] ^io.input-link $binding)"
    }
    namespace export bind-input

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
        return "([_state] ^io.output-link $binding)"
    }
    namespace export bind-output

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
    namespace export _bind

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
    namespace export match

    proc _modify { type_ block { binding_ "" } } {
        set type $type_

        if { $binding_ eq "" } {
            set binding [_lookup_default_binding $type]
        } else {
            set binding $binding_
        }

        return [subst $block]
    }
    namespace export _modify

    proc add { args } {
        upvar type type
        upvar binding binding
        if { [llength $args] < 2 || [expr [llength $args] % 2] != 0 } {
            _dump_error $type add rhs "usage: (^member <value>)+"
        }
        if { $_in_operator } {
            set support o
        } else  {
            set support i
        }

        set ret ""
        if { !$_in_operator } {
            set ret "($binding"
        }
        # TODO: Compute multiple
        # TODO: Support tagging
        foreach { member_ value } $args {
            _check_member $type add $member_ $value 0 1 0 $support 0
            set member [_soar_attribute_to_ngs $member_]
            if { $_in_operator } {
                set action [_get_ngs_action $type $member]
                if { $_first_operator_action } {
                    variable _first_operator_action 0
                    set ret "$ret[ngs-create-attribute-by-operator [_state] $binding $member $value $action $_operator_preferences"
                } else {
                    set ret "$ret[ngs-add-primitive-side-effect $::NGS_SIDE_EFFECT_ADD $binding $member $value $action]"
                }
            } else {
                set ret "$ret $member $value"
            }
        }

        if { !$_in_operator } {
            set ret "$ret)"
        }

        return $ret
    }

    proc remove { args } {
        upvar type type
        upvar binding binding
        if { [llength $args] < 2 || [expr [llength $args] % 2] != 0 } {
            _dump_error $type remove rhs "usage: (^member <value>)+"
        }
        if { $_in_operator } {
            set support o
        } else  {
            set support i
        }
        set ret ""
        if { !$_in_operator } {
            set ret "($binding"
        }
        # TODO: Tags
        foreach { member_ value } $args {
            _check_member $type remove $member_ $value 0 0 0 $support 0
            set member [_soar_attribute_to_ngs $member_]
            if { $_in_operator } {
                if { $_first_operator_action } {
                        variable _first_operator_action 0
                        set ret "$ret[ngs-remove-attribute-by-operator [_state] $binding $member ]"
                } else {
                    set ret "$ret[ngs-add-primitive-side-effect $::NGS_SIDE_EFFECT_REMOVE $binding $member $value]"
                }
            } else {
                set ret "$ret $member $value -"
            }
        }

        if { !$_in_operator } {
            set ret "$ret)"
        }

        return $ret
    }

    proc _construct { type action arguments } {
        # TODO: Should be able to inferere action, also check the binding ^attribute this
        if { [llength $arguments] < 4 || [expr [llength $arguments] % 2 != 0] } {
            _dump_error $type construct rhs "usage: binding ^attribute (^member value)+ (as <binding>)?"
        }
        if { $_in_operator } {
            set support "o"
        } else {
            set support "i"
        }

        set binding ""
        set parent_binding [lindex $arguments 0]
        set attribute [_soar_attribute_to_ngs [lindex $arguments 1]]
        set required [_all_const_members $type]
        set additions [dict create]
        set attrs [list]
        foreach { member_ value } $arguments {
            if { $member_ eq "as" } {
                if { $binding ne "" } {
                    _dump_error $type construct rhs "as <binding> must be the final entry"
                }
                set binding $value
            } else {
                set member [_soar_attribute_to_ngs $member_]
                if {[dict exists $additions $member]} {
                    set multi_add [dict get $additions $member]
                } else {
                    set multi_add 0
                }
                _check_member $type construct $member $value 0 1 1 $support $multi_add
                incr multi_add
                dict set additions $member $multi_add
                lappend attrs [_soar_attribute_to_ngs $member] $value
            }
        }

        foreach member $required {
            if { ![dict exists $additions $member] } {
                _dump_error $type construct rhs "member $member is required (const non-optional) but not set"
            }
        }

        if { $binding eq "" } {
            set binding [_next_anonymous_binding $type]
        }

        if { $_in_operator } {
            if { $_first_operator_action } {
                variable _first_operator_action 0
                return [ngs-create-typed-object-by-operator [_state] $parent $attribute [set ${type}::type] $binding $attrs $action $_operator_preferences]
            } else {
                return [ngs-create-typed-sub-object-by-operator $parent $attribute [set ${type}::type] $binding $attrs]
            }
        } else {
            return [ngs-create-typed-object $parent $attribute [set ${type}::type] $binding $attrs]
        }
    }

    proc _deploy { type arguments } {
        if { [llength $arguments] != 2 } {
            _dump_error $type deploy rhs "usage: ^attribute <binding>"
        }
        variable _first_operator_action
        set attribute [_soar_attribute_to_ngs [lindex $arguments 0]]
        set binding [lindex $arguments 1]

        if { $_first_operator_action } {
            variable _first_operator_action 0
            return [ngs-deep-copy-by-operator [_state] [_output_link] $attribute $binding $::NGS_ADD_TO_SET]
        } else {
            _dump_error $type deploy rhs "deploy must be the first action for this operator (and preferably only)"
        }
    }

    proc _get_ngs_action { type member_ } {
        set member [_soar_attribute_to_ngs $member_]
        if { [dict get ${[set ${type}::members]} multiple] eq "true" } {
            return $::NGS_ADD_TO_SET
        } else {
            return $::NGS_REPLACE_IF_EXISTS
        }
    }

    proc _check_member { type function member_ value tag add creation support multi_add } {
        set member [_soar_attribute_to_ngs $member_]
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

    # Returns a list of all required (const and non-optional) members for a given type.
    proc _all_required_members { type } {
        set res [list]
        foreach { member definition } [array get [set ${type}::members]] {
            if { [dict get $definition const] eq "true" && [dict get $definition optional] ne "true" } {
                lappend res $member
            }
        }
        return $res
    }

    proc _dump_error { type func side message } {
        # TODO: Look up the call chain for func, possibly side
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

    proc _input_link { } {
        return "<input-link>"
    }
    namespace export _input_link

    proc _output_link { } {
        return "<output-link>"
    }
    namespace export _output_link

    proc _assert_lhs { func } {
        if { !$_in_lhs } {
            _dump_error "" $func lhs "must be within a LHS block"
        }
    }

    proc _reset_lhs { } {
        variable ::Glide::_in_hs 0
        array set _bindings {}
        array set _anonymous_bindings
    }
    namespace export _reset_lhs

    proc _reset_operator { } {
        variable ::Glide::_in_operator 0
        variable _first_operator_action ""
        variable _operator_preference ""
    }
    namespace export _reset_operator
}