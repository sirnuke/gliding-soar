# Manually opens a LHS context, with root_binding as the root binding
proc lhs { root_binding block } {
    set binding $root_binding

    return [subst $block]
}

namespace eval Glide {
    namespace export _bind _match _set _apply _add _remove _construct _initiate _deploy

    proc _dump_error { type func side message } {
        # TODO: We might know the production name and scope, once the production wrappers are implemented
        error "Bad $side $type.$func call: $message"
    }

    proc _claim_binding { binding type } {
        # TODO: Stub!
    }

    proc _next_anonymous_binding { type } {
        # TODO: Add numeric!
        return "<[set ${type}::identifier]>"
    }

    proc _default_attribute { type } {
        return "^[set ${type}::identifier]"
    }

    proc _bind { type_ arguments } {
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
        # TODO: Stub!
    }

    proc _apply { type arguments } {
        # TODO: Stub!
    }

    proc _add { type arguments } {
        # TODO: Stub!
    }

    proc _remove { type arguments } {
        # TODO: Stub!
    }

    proc _construct { type arguments } {
        # TODO: Stub!
    }

    proc _initiate { type arguments } {
        # TODO: Stub!
    }

    proc _deploy { type arguments } {
        # TODO: Stub!
    }
}