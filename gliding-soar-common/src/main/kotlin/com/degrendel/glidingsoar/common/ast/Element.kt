package com.degrendel.glidingsoar.common.ast

sealed class Element : ASTNode
{
  final override val children = ArrayList<ASTNode>()
  abstract val identifier: Identifier
  abstract val extends: List<Identifier>
  abstract val body: Body
}

data class Input(override val location: Location, override val identifier: Identifier, override val extends: List<Identifier>, override val body: Body) : Element()
{
  init
  {
    children.add(identifier)
    children.addAll(extends)
    children.add(body)
  }
}

data class Object(override val location: Location, override val identifier: Identifier, override val extends: List<Identifier>, override val body: Body) : Element()
{
  init
  {
    children.add(identifier)
    children.addAll(extends)
    children.add(body)
  }
}

data class Output(override val location: Location, override val identifier: Identifier, override val extends: List<Identifier>, override val body: Body) : Element()
{
  init
  {
    children.add(identifier)
    children.addAll(extends)
    children.add(body)
  }
}

data class Interface(override val location: Location, override val identifier: Identifier, override val extends: List<Identifier>, override val body: Body) : Element()
{
  init
  {
    children.add(identifier)
    children.addAll(extends)
    children.add(body)
  }
}

/*
data class Element(override val location: Location, val declaration: Declaration, val body: Body) : ASTNode
{
  override val children = ArrayList<ASTNode>()

  init
  {
    children.add(declaration)
    children.add(body)
  }
}
 */
