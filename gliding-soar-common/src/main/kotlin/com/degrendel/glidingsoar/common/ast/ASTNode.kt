package com.degrendel.glidingsoar.common.ast

interface ASTNode
{
  val children: List<ASTNode>
  val location: Location
}

data class Location(val source: String, val line: Int, val offset: Int)
{
  override fun toString() = "$source@$line:$offset"
}

data class Identifier(override val location: Location, val value: String) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  val lowercase = value.toLowerCase()

  override fun toString() = value
}

data class ResolvedIdentifier(override val location: Location, val namespace: List<Identifier>, val value: String) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.addAll(namespace)
  }

  val lowercase = value.toLowerCase()

  override fun toString() = "${namespace.map { "${it.value}." }}$value"
}

data class RawTcl(override val location: Location, val block: String) : ASTNode
{
  override val children = ArrayList<ASTNode>()
}
