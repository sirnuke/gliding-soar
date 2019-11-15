package com.degrendel.glidingsoar.common.ast

data class Body(override val location: Location, val members: List<Member>, val tags: List<Tag>, val matches: List<Match>) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.addAll(members)
    children.addAll(tags)
    children.addAll(matches)
  }
}

data class Member(override val location: Location) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}

data class Tag(override val location: Location) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}

data class Match(override val location: Location) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}
