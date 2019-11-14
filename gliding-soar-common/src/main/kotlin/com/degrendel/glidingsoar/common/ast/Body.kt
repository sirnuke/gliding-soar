package com.degrendel.glidingsoar.common.ast

data class Body(override val location: Location) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}