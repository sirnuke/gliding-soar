package com.degrendel.glidingsoar.common

import com.degrendel.glidingsoar.common.ast.ASTNode
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

class GlideParseException(message: String) : Exception(message)