package com.degrendel.glidingsoar.common.ast

interface ASTNode
{
  val children: List<ASTNode>
  val location: Location
}

data class Location(val line: Int, val offset: Int)

data class Identifier(override val location: Location, val value: String) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}
