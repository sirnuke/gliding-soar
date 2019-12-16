package com.degrendel.glidingsoar.common

import com.degrendel.glidingsoar.common.ast.ASTNode
import com.degrendel.glidingsoar.common.ast.Element
import com.degrendel.glidingsoar.common.ast.Identifier
import java.net.URI

interface Model
{
  fun bundle(): String
  fun parseFile(uri: URI)
  fun parseString(source: String, contents: String)
}

class DuplicateSymbolException(message: String, name: String, first: ASTNode, second: ASTNode)
  : Exception("Duplicate symbol reference of '$name' @ ${first.location} and ${second.location}: $message")

class UnknownSymbolException(message: String, name: String, reference: ASTNode)
  : Exception("Unknown symbol reference of '$name' @ ${reference.location}: $message")

class UnknownNamespaceException(message: String, name: String, reference: ASTNode)
  : Exception("Missing namespace '$name' @ ${reference.location}: $message")

// TODO: Probably better ways of reporting this (low priority, proper solution is Don't Use Complex Inheritance)
class InheritanceCycleException(message: String, starting: Element, cycle: Element)
  : Exception("Inheritance circular reference in '${starting.identifier}' @ ${starting.location} in ${cycle.identifier} @ ${cycle.location}: $message")

class DuplicateExtendsException(message: String, element: Element, parent: Element)
  : Exception("Element ${element.identifier} @ ${element.location} extends ${parent.identifier} multiple times: $message")

class IncompatibleMembersException(message: String, element: Element, first: Identifier, second: Identifier)
  : Exception("Element ${element.identifier} has duplicate incompatible members $first @ ${first.location} $second @ ${second.location}: $message")

class InvalidExtendsTypeException(message: String, element: Element, parent: Element)
  : Exception("Element ${element.identifier} @ ${element.location} of type ${element.type} cannot extend element ${parent.identifier} of type ${parent.type}: $message")

class GlideParseException(message: String) : Exception(message)