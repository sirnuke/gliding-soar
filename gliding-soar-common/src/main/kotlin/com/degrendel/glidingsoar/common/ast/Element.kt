package com.degrendel.glidingsoar.common.ast

data class Element(override val location: Location) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}
