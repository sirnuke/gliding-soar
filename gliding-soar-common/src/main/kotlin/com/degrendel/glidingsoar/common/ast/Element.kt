package com.degrendel.glidingsoar.common.ast

import com.degrendel.glidingsoar.common.InvalidExtendsTypeException

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

  val tangible: Boolean get() = type != ElementType.INTERFACE

  private val _allMembers = mutableListOf<Member>()
  val allMembers: List<Member> get() = _allMembers

  private val _types = mutableListOf<ResolvedIdentifier>()
  val types: List<ResolvedIdentifier> get() = _types

  private val membersByName = mutableMapOf<String, Member>()

  init
  {
    children.add(identifier)
    children.addAll(extends)
    _allMembers.addAll(members)
    members.forEach { membersByName[it.identifier.value] = it }
  }

  fun resolve(parents: Set<Element>)
  {
    parents.forEach {
      _types.add(it.identifier)
      when (it.type)
      {
        ElementType.INTERFACE -> { }
        ElementType.OBJECT ->
          if (type == ElementType.INTERFACE)
            throw InvalidExtendsTypeException("Interfaces can only extend other interfaces", this, it)
        ElementType.OUTPUT ->
          if (type != ElementType.OUTPUT)
            throw InvalidExtendsTypeException("Only output elements can extend other outputs", this, it)
        ElementType.INPUT ->
          if (type != ElementType.INPUT)
            throw InvalidExtendsTypeException("Only input elements can extend other inputs", this, it)
      }
      it.members.forEach { candidate ->
        val existing = membersByName[candidate.identifier.value]
        if (existing != null)
        {
          existing.checkOverride(candidate)
          return
        }
        _allMembers.add(candidate)
        membersByName[candidate.identifier.value] = candidate
      }
    }
  }

  override fun toString(): String
  {
    return "Element $type $identifier $location : ${extends.joinToString(", ")} ${members.size} members ${matches.size} matches"
  }
}

