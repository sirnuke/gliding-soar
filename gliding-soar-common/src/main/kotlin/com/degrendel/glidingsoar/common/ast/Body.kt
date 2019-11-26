package com.degrendel.glidingsoar.common.ast

data class Body(override val location: Location, val parameters: List<Parameter>, val members: List<Member>, val matches: List<Match>) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.addAll(parameters)
    children.addAll(members)
    children.addAll(matches)
  }
}

data class Parameter(override val location: Location) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}

data class Member(override val location: Location) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}

data class Match(override val location: Location) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}
