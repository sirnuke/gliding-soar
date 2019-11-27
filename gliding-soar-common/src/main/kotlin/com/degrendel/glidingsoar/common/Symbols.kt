package com.degrendel.glidingsoar.common

import com.degrendel.glidingsoar.common.ast.Element

sealed class Symbol
{
  abstract val name: String
}

data class NamespaceSymbol(val namespace: ChildNamespace) : Symbol()
{
  override val name = namespace.name
}

data class ElementSymbol(val element: Element): Symbol()
{
  override val name = element.declaration.identifier.value
}

sealed class Namespace
{
  abstract val name: String
  abstract val fullyQualified: String

  private val _symbols = mutableMapOf<String, Symbol>()
  val symbols: Map<String, Symbol>
    get() = _symbols

  private val _children = mutableListOf<ChildNamespace>()
  val children: List<ChildNamespace>
    get() = _children

  private val _elements = mutableListOf<Element>()
  val elements: List<Element>
    get() = _elements

  fun addNamespace(namespace: ChildNamespace)
  {
    _children.add(namespace)
    addSymbol(NamespaceSymbol(namespace))
  }

  fun addElement(element: Element)
  {
    _elements.add(element)
    addSymbol(ElementSymbol(element))
  }

  private fun addSymbol(symbol: Symbol)
  {
    if (symbol.name in _symbols)
      throw IllegalStateException("Attempting to add new symbol ${symbol.name} ($symbol), conflicts with existing symbol ${_symbols[symbol.name]}")
   _symbols[name] = symbol
  }
}

class RootNamespace : Namespace()
{
  override val name = ""
  override val fullyQualified = ""
}

data class ChildNamespace(val parent: Namespace, override val name: String) : Namespace()
{
  override val fullyQualified: String = if (parent is RootNamespace)
    name
  else
    "${parent.fullyQualified}.$name"
}

