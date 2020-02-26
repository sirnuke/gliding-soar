package com.degrendel.glidingsoar.service

import com.degrendel.glidingsoar.common.ast.*
import com.degrendel.glidingsoar.service.grammar.GlidingSoarBaseVisitor
import com.degrendel.glidingsoar.service.grammar.GlidingSoarLexer
import com.degrendel.glidingsoar.service.grammar.GlidingSoarParser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.TerminalNode

sealed class ParserResults
data class ParseSuccess(val elements: List<Element>) : ParserResults()
data class ParseFailure(val location: Location, val message: String) : ParserResults()
{
  fun toHumanString() =
    "Unable to parse at ${location.line}@${location.offset}"
}

class BlockParser : GlidingSoarBaseVisitor<List<Element>>()
{
  private lateinit var source: Location

  private val parserErrorListener = object : BaseErrorListener()
  {
    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?)
    {
      // TODO: Off by one error (is the first line of block 1 or 0?)
      source = Location(source.source, line + source.line, charPositionInLine)
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

  override fun visitGlidingSoar(ctx: GlidingSoarParser.GlidingSoarContext): List<Element> = ctx.element().map { visitor.visitElement(it) }

  private val visitor = object : GlidingSoarBaseVisitor<ASTNode>()
  {
    override fun visitElement(ctx: GlidingSoarParser.ElementContext): Element
    {
      val identifier = visitResolvedIdentifier(ctx.resolvedIdentifier())
      val extends = if (ctx.extends_() == null)
        ArrayList()
      else
        ctx.extends_().resolvedIdentifier().map { visitResolvedIdentifier(it) }
      val members = ctx.body().bodyElement().filter { it.member() != null }.map { visitMember(it.member()) }
      val matches = ctx.body().bodyElement().filter { it.match() != null }.map { visitMatch(it.match()) }
      val type = when
      {
        ctx.type().INPUT() != null -> Element.ElementType.INPUT
        ctx.type().INTERFACE() != null -> Element.ElementType.INTERFACE
        ctx.type().OUTPUT() != null -> Element.ElementType.OUTPUT
        ctx.type().OBJECT() != null -> Element.ElementType.OBJECT
        else -> throw IllegalStateException("Found element $ctx with unknown/unhandled type")
      }
      return Element(symbolToLocation(ctx.start), type, identifier, extends, members, matches)
    }

    override fun visitResolvedIdentifier(ctx: GlidingSoarParser.ResolvedIdentifierContext): ResolvedIdentifier
    {
      val namespace = ctx.IDENTIFIER().subList(0, ctx.IDENTIFIER().size - 1).map { convertIdentifier(it) }
      return ResolvedIdentifier(symbolToLocation(ctx.start), namespace, ctx.IDENTIFIER().last().text)
    }

    override fun visitMember(ctx: GlidingSoarParser.MemberContext): Member
    {
      val identifier = convertIdentifier(ctx.IDENTIFIER())
      val type = visitResolvedIdentifier(ctx.resolvedIdentifier())
      val tag = (ctx.TAG() != null)
      val const = (ctx.CONST().size > 0)
      val optional = (ctx.OPTIONAL().size > 0)
      val multiple = (ctx.MULTIPLE().size > 0)
      if (ctx.I_SUPPORT() != null && ctx.O_SUPPORT() != null)
        throw IllegalStateException("Found member that is both o support and i support!: $ctx")
      val support = when
      {
        ctx.I_SUPPORT() != null -> Member.SupportType.ISUPPORT
        ctx.O_SUPPORT() != null -> Member.SupportType.OSUPPORT
        else -> Member.SupportType.UNRESTRICTED
      }
      return Member(symbolToLocation(ctx.start), support = support, tag = tag, identifier = identifier, type = type, const = const, optional = optional, multiple = multiple)
    }

    override fun visitMatch(ctx: GlidingSoarParser.MatchContext): Match
    {
      val identifier = convertIdentifier(ctx.IDENTIFIER())
      val block = convertRawTcl(ctx.RAW_TCL())
      if (ctx.arguments() != null)
        TODO("Arguments not yet implemented!")
      return when
      {
        ctx.PROC() != null -> Proc(symbolToLocation(ctx.start), identifier, block)
        ctx.SUBST() != null -> Subst(symbolToLocation(ctx.start), identifier, block)
        else -> throw IllegalStateException("Found match that is neither proc nor subst!: $ctx")
      }
    }

    fun convertIdentifier(identifier: TerminalNode) = Identifier(symbolToLocation(identifier.symbol), identifier.symbol.text)
    fun convertRawTcl(rawTcl: TerminalNode) = RawTcl(symbolToLocation(rawTcl.symbol), rawTcl.symbol.text)
  }

  fun symbolToLocation(symbol: Token) = Location(source.source, source.line + symbol.line, symbol.charPositionInLine)
}
