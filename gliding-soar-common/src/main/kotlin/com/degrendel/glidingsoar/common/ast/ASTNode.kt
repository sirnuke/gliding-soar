package com.degrendel.glidingsoar.common.ast

interface ASTNode
{
  val children: List<ASTNode>
  val location: Location
}

data class Location(val line: Int, val offset: Int)
