package com.degrendel.glidingsoar.common.ast

import com.degrendel.glidingsoar.common.DuplicateExtendsException
import com.degrendel.glidingsoar.common.InheritanceCycleException
import com.degrendel.glidingsoar.common.InvalidExtendsTypeException
import com.degrendel.glidingsoar.common.RootNamespace

class Element(override val location: Location, val type: ElementType, val identifier: ResolvedIdentifier, val extends: List<ResolvedIdentifier>, val members: List<Member>, val matches: List<Match>) : ASTNode
{
  enum class ElementType(val label: String)
  {
    INPUT("input"),
    OUTPUT("output"),
    INTERFACE("interface"),
    OBJECT("object"),
  }

  override val children = ArrayList<ASTNode>()

  private var _resolved = false
  val resolved get() = _resolved

  val tangible: Boolean get() = type != ElementType.INTERFACE

  init
  {
    children.add(identifier)
    children.addAll(extends)
  }

  fun resolve(root: RootNamespace, chain: List<Element>)
  {
    if (resolved)
      throw IllegalStateException("Attempting to resolve already resolved element $this")

    if (chain.contains(this))
      throw InheritanceCycleException("Unable to resolve symbols", chain.first(), this)

    val namespace = root.resolveNamespace(this, identifier)

    val parentsList = extends.map { namespace.resolveElement(this, it) }
    val parents = parentsList.toSet()
    val extendedChain = chain.plus(this)
    parentsList.minus(parents).forEach { throw DuplicateExtendsException("Multiple references in extends", this, it) }
    parents.filterNot { it.resolved }.forEach { it.resolve(root, extendedChain) }

    // TODO: This feels 10x more verbose than it needs to be
    parents.forEach { extendWith(it) }

    _resolved = true
  }

  private fun extendWith(parent: Element)
  {
    when (parent.type)
    {
      ElementType.INTERFACE ->
      {
      }
      ElementType.OBJECT ->
        if (type == ElementType.INTERFACE)
          throw InvalidExtendsTypeException("Interfaces can only extend other interfaces", this, parent)
      ElementType.OUTPUT ->
        if (type != ElementType.OUTPUT)
          throw InvalidExtendsTypeException("Only output elements can extend other outputs", this, parent)
      ElementType.INPUT ->
        if (type != ElementType.INPUT)
          throw InvalidExtendsTypeException("Only input elements can extend other inputs", this, parent)
    }
    // TODO: Iterate over members and matches, check compatibility, and add to list as necessary
    TODO("Actual extends isn't implemented!")
  }
}

