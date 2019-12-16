package com.degrendel.glidingsoar.common.ast

// TODO: Arguments
sealed class Match : ASTNode
{
  abstract val identifier: Identifier
  abstract val block: RawTcl
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

data class Subst(override val location: Location, override val identifier: Identifier, override val block: RawTcl) : Match()
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.add(identifier)
    children.add(block)
  }
}

