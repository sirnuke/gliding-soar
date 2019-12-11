package com.degrendel.glidingsoar.common

import com.degrendel.glidingsoar.common.ast.ASTNode
import com.degrendel.glidingsoar.common.ast.Element
import com.degrendel.glidingsoar.common.ast.Identifier

sealed class Symbol
{
  abstract val name: String
  abstract val firstReference: ASTNode
}

data class NamespaceSymbol(val namespace: ChildNamespace, override val firstReference: ASTNode) : Symbol()
{
  override val name = namespace.name
}

data class ElementSymbol(val element: Element) : Symbol()
{
  override val name = element.identifier.value
  override val firstReference = element
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

  private fun addNamespaceSymbol(namespace: ChildNamespace, reference: ASTNode): NamespaceSymbol
  {
    _children.add(namespace)
    val symbol = NamespaceSymbol(namespace, reference)
    addSymbol(symbol)
    return symbol
  }

  private fun addElementSymbol(element: Element): ElementSymbol
  {
    _elements.add(element)
    val symbol = ElementSymbol(element)
    addSymbol(symbol)
    return symbol
  }

  protected fun addElement(path: Iterator<Identifier>, element: Element)
  {
    if (!path.hasNext())
    {
      val existing = _symbols[element.identifier.value]
      if (existing != null)
      {
        val message = when (existing)
        {
          is NamespaceSymbol -> "Symbol already referenced as namespace"
          is ElementSymbol -> "Symbol already defined as element"
        }
        throw DuplicateSymbolException(message, element.identifier.value, existing.firstReference, element)
      }
      addElementSymbol(element)
    }
    else
    {
      val child = path.next()
      val symbol = _symbols[child.value]
      val childNamespace = if (symbol != null)
      {
        when (symbol)
        {
          is ElementSymbol -> throw DuplicateSymbolException("Symbol referenced as namespace, but exists as element", child.value, symbol.element, element)
          is NamespaceSymbol -> symbol.namespace
        }
      }
      else
        addNamespaceSymbol(ChildNamespace(this, child.value), element).namespace
      childNamespace.addElement(path, element)
    }
  }

  private fun addSymbol(symbol: Symbol)
  {
    if (symbol.name in _symbols)
      throw IllegalStateException("Attempting to add new symbol ${symbol.name} ($symbol), conflicts with existing symbol ${_symbols[symbol.name]}")
    _symbols[name] = symbol
  }

  internal fun validate(root: RootNamespace)
  {
    elements.forEach {
      if (it.extends.isNotEmpty())
        TODO("Inheritance/interfaces are not yet implemented")
    }
    // TODO: Confirm not invalid members (i.e. elaboratables or matches on output)
    children.forEach { it.validate(root) }
  }
}

class RootNamespace : Namespace()
{
  companion object
  {
    val L by logger()
  }

  override val name = ""
  override val fullyQualified = ""

  fun addElements(elements: List<Element>)
  {
    elements.forEach { addElement(it.identifier.namespace.iterator(), it) }
  }

  fun validate()
  {
    L.info("Validating namespace symbols")
    validate(this)
  }
}

data class ChildNamespace(val parent: Namespace, override val name: String) : Namespace()
{
  override val fullyQualified: String = if (parent is RootNamespace)
    name
  else
    "${parent.fullyQualified}.$name"
}

