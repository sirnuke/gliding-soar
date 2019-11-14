package com.degrendel.glidingsoar.common.ast

data class Element(override val location: Location, val declaration: Declaration, val body: Body) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.add(declaration)
    children.add(body)
  }
}
