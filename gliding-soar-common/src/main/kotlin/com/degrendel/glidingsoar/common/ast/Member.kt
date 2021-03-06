package com.degrendel.glidingsoar.common.ast

data class Member(override val location: Location, val support: SupportType, val tag: Boolean, val identifier: Identifier, val type: ResolvedIdentifier, val optional: Boolean, val const: Boolean, val multiple: Boolean) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  enum class SupportType(val label: String)
  {
    ISUPPORT("i"),
    OSUPPORT("o"),
    UNRESTRICTED("any"),
  }

  init
  {
    children.add(identifier)
    children.add(type)
  }

  fun checkOverride(other: Member)
  {
    assert(other.identifier.value == identifier.value)
    TODO("Stub!")
  }
}

