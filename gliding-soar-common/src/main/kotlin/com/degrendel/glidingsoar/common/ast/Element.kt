package com.degrendel.glidingsoar.common.ast

sealed class Element : ASTNode
{
  final override val children = ArrayList<ASTNode>()
  abstract val identifier: ResolvedIdentifier
  abstract val extends: List<ResolvedIdentifier>
  abstract val body: Body

  open val tangible = true
}

data class Input(override val location: Location, override val identifier: ResolvedIdentifier, override val extends: List<ResolvedIdentifier>, override val body: Body) : Element()
{
  init
  {
    children.add(identifier)
    children.addAll(extends)
    children.add(body)
  }
}

data class Object(override val location: Location, override val identifier: ResolvedIdentifier, override val extends: List<ResolvedIdentifier>, override val body: Body) : Element()
{
  init
  {
    children.add(identifier)
    children.addAll(extends)
    children.add(body)
  }
}

data class Output(override val location: Location, override val identifier: ResolvedIdentifier, override val extends: List<ResolvedIdentifier>, override val body: Body) : Element()
{
  init
  {
    children.add(identifier)
    children.addAll(extends)
    children.add(body)
  }
}

data class Interface(override val location: Location, override val identifier: ResolvedIdentifier, override val extends: List<ResolvedIdentifier>, override val body: Body) : Element()
{
  init
  {
    children.add(identifier)
    children.addAll(extends)
    children.add(body)
  }

  override val tangible = false
}
