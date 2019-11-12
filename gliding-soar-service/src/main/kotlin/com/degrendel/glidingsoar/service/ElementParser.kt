package com.degrendel.glidingsoar.service

import com.degrendel.glidingsoar.common.ast.Element
import com.degrendel.glidingsoar.common.ast.Location
import com.degrendel.glidingsoar.service.grammar.GlidingSoarBaseVisitor
import com.degrendel.glidingsoar.service.grammar.GlidingSoarLexer
import com.degrendel.glidingsoar.service.grammar.GlidingSoarParser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException

sealed class ParserResults
data class ParseSuccess(val elements: List<Element>) : ParserResults()
data class ParseFailure(val location: Location, val message: String) : ParserResults()

class ElementParser : GlidingSoarBaseVisitor<List<Element>>()
{
  private lateinit var source: Location

  private val parserErrorListener = object : BaseErrorListener()
  {
    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?)
    {
      // TODO: Off by one error (is the first line of block 1 or 0?)
      source = Location(line + source.line, charPositionInLine)
      throw ParseCancellationException("Grammar exception: $msg")
    }
  }

  fun parse(source: Location, block: String): ParserResults
  {
    this.source = source
    return try
    {
      val charStreams = CharStreams.fromString(block)
      val lexer = GlidingSoarLexer(charStreams)
      lexer.removeErrorListeners()
      lexer.addErrorListener(parserErrorListener)
      val tokens = CommonTokenStream(lexer)
      val parser = GlidingSoarParser(tokens)
      parser.removeErrorListeners()
      parser.addErrorListener(parserErrorListener)
      return ParseSuccess(visit(parser.glidingSoar()))
    }
    catch (e: ParseCancellationException)
    {
      ParseFailure(this.source, "Unable to parse!: ${e.localizedMessage}")
    }
  }

  override fun visitGlidingSoar(ctx: GlidingSoarParser.GlidingSoarContext): List<Element> = ctx.element().map { elementVisitor.visitElement(it) }

  private val elementVisitor = object : GlidingSoarBaseVisitor<Element>()
  {
    override fun visitElement(ctx: GlidingSoarParser.ElementContext?): Element
    {
      TODO("Stub!")
    }
  }
}
