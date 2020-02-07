package com.degrendel.glidingsoar.common

import com.degrendel.glidingsoar.common.ast.ASTNode
import com.degrendel.glidingsoar.common.ast.Element
import com.degrendel.glidingsoar.common.ast.Identifier
import com.degrendel.glidingsoar.common.ast.ResolvedIdentifier
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedAcyclicGraph
import java.lang.IllegalArgumentException

sealed class Symbol
{
  abstract val name: String
  abstract val firstReference: ASTNode
  abstract val fullyQualified: String
}

data class NamespaceSymbol(val namespace: ChildNamespace, override val firstReference: ASTNode) : Symbol()
{
  override val name = namespace.name

  override val fullyQualified: String
    get() = namespace.fullyQualified
}

data class ElementSymbol(val element: Element, val namespace: Namespace) : Symbol()
{
  override val name = element.identifier.value
  override val firstReference = element

  override val fullyQualified: String
    get() =
      if (namespace is RootNamespace)
        name
      else
        "${namespace.fullyQualified}.name"
}

sealed class Namespace()
{
  internal abstract val root: RootNamespace
  abstract val name: String
  abstract val fullyQualified: String

  private val symbols = mutableMapOf<String, Symbol>()
  private val _children = mutableListOf<ChildNamespace>()
  private val _elements = mutableListOf<Element>()
  val children: List<ChildNamespace> get() = _children
  val elements: List<Element> get() = _elements


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
    val symbol = ElementSymbol(element, this)
    addSymbol(symbol)
    return symbol
  }

  protected fun addElement(path: Iterator<Identifier>, element: Element): ElementSymbol
  {
    return if (!path.hasNext())
    {
      val existing = symbols[element.identifier.value]
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
      val symbol = symbols[child.value]
      val childNamespace = if (symbol != null)
      {
        when (symbol)
        {
          is ElementSymbol -> throw DuplicateSymbolException("Symbol referenced as namespace, but exists as element", child.value, symbol.element, element)
          is NamespaceSymbol -> symbol.namespace
        }
      }
      else
        addNamespaceSymbol(ChildNamespace(root, this, child.value), element).namespace
      childNamespace.addElement(path, element)
    }
  }

  private fun addSymbol(symbol: Symbol)
  {
    if (symbol.name in symbols)
      throw IllegalStateException("Attempting to add new symbol ${symbol.name} ($symbol), conflicts with existing symbol ${symbols[symbol.name]}")
    symbols[name] = symbol
  }

  protected fun recurseNamespace(reference: ASTNode, namespace: List<Identifier>): Namespace
  {
    return if (namespace.isEmpty())
      this
    else
    {
      (symbols[namespace.first().value] as? NamespaceSymbol)?.namespace?.recurseNamespace(reference, namespace.drop(1))
          ?: throw UnknownNamespaceException("Namespace not found!", namespace.first().value, reference)
    }
  }

  fun resolveElement(reference: ASTNode, symbol: ResolvedIdentifier): Element
  {
    return if (symbol.namespace.isEmpty())
      getElement(reference, symbol)
    else
      resolveNamespace(reference, symbol).getElement(reference, symbol)
  }

  fun resolveNamespace(reference: ASTNode, symbol: ResolvedIdentifier): Namespace = root.recurseNamespace(reference, symbol.namespace)

  private fun getElement(reference: ASTNode, symbol: ResolvedIdentifier): Element
  {
    return (symbols[symbol.value] as? ElementSymbol)?.element
        ?: throw UnknownSymbolException("Symbol not found!", symbol.toString(), reference)
  }
}

class RootNamespace : Namespace()
{
  companion object
  {
    val L by logger()
  }

  private fun resolve(identifier: ResolvedIdentifier, reference: ElementSymbol, root: RootNamespace): Element
  {
    val namespace = if (identifier.namespace.isEmpty())
      root
    else
      root.resolveNamespace(identifier, identifier)
    val results = namespace.elements.filter { it.identifier.value == identifier.value }
    return when
    {
      results.isEmpty() -> throw UnknownSymbolException("Error while resolving type hierarchy", identifier.value, identifier)
      results.size > 1 -> throw IllegalStateException("Found multiple symbol elements in ${reference.namespace} with identifier ${identifier.value}")
      else -> results.first()
    }
  }

  private val hierarchy = DirectedAcyclicGraph<Element, DefaultEdge>(DefaultEdge::class.java)
  private val allElements = ArrayList<ElementSymbol>()

  override val root = this
  override val name = ""
  override val fullyQualified = ""

  fun resolve()
  {
    allElements.forEach {
      try
      {
        it.element.extends.forEach { extend -> hierarchy.addEdge(it.element, resolve(extend, it, this)) }
      }
      catch (e: IllegalArgumentException)
      {
        L.error("Cycle detected", e)
        TODO("Handle cycles!")
      }
    }
    allElements.forEach { it.element.resolve(hierarchy.getDescendants(it.element)) }
  }

  fun addElements(elements: List<Element>)
  {
    elements.forEach {
      hierarchy.addVertex(it)
      allElements.add(addElement(it.identifier.namespace.iterator(), it))
    }
  }
}

data class ChildNamespace(override val root: RootNamespace, val parent: Namespace, override val name: String) : Namespace()
{
  override val fullyQualified: String = if (parent is RootNamespace)
    name
  else
    "${parent.fullyQualified}.$name"
}

