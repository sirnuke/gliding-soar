package com.degrendel.glidingsoar.common.ast

data class Declaration(override val location: Location, val type: Type, val identifier: Identifier, val extends: List<Identifier>) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.add(type)
    children.add(identifier)
    children.addAll(extends)
  }
}

sealed class Type() : ASTNode
{
  override val children = ArrayList<ASTNode>()
}

data class Input(override val location: Location) : Type()
data class Object(override val location: Location) : Type()
data class Output(override val location: Location) : Type()
data class Interface(override val location: Location) : Type()

