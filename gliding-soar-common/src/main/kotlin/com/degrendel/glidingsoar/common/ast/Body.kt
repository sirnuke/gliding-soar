package com.degrendel.glidingsoar.common.ast

data class Body(override val location: Location, val parameters: List<Parameter>, val members: List<Member>, val matches: List<Match>) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  val allParameters = mutableListOf<Parameter>()

  init
  {
    children.addAll(parameters)
    children.addAll(members)
    children.addAll(matches)

    allParameters.addAll(parameters)
  }
}

data class Parameter(override val location: Location, val identifier: Identifier, val type: Identifier, val multiple: Boolean, val optional: Boolean) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.add(identifier)
    children.add(type)
  }
}

sealed class Member : ASTNode
{
  abstract val identifier: Identifier
  abstract val type: Identifier
  abstract val tag: Boolean
  abstract val multiple: Boolean
}

data class IMember(override val location: Location, override val tag: Boolean, override val identifier: Identifier, override val type: Identifier, override val multiple: Boolean) : Member()
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.add(identifier)
    children.add(type)
  }
}

data class OMember(override val location: Location, override val tag: Boolean, override val identifier: Identifier, override val type: Identifier, override val multiple: Boolean) : Member()
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.add(identifier)
    children.add(type)
  }
}

// TODO: Arguments
sealed class Match : ASTNode
{
  abstract val identifier: Identifier
  abstract val block: RawTcl
}

data class Subst(override val location: Location, override val identifier: Identifier, override val block: RawTcl) : Match()
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.add(identifier)
    children.add(block)
  }
}

data class Proc(override val location: Location, override val identifier: Identifier, override val block: RawTcl) : Match()
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.add(identifier)
    children.add(block)
  }
}
