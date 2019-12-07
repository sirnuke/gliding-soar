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

class BlockParser : GlidingSoarBaseVisitor<List<Element>>()
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

  override fun visitGlidingSoar(ctx: GlidingSoarParser.GlidingSoarContext): List<Element> = ctx.element().map { visitor.visitElement(it) }

  private val visitor = object : GlidingSoarBaseVisitor<ASTNode>()
  {
    override fun visitElement(ctx: GlidingSoarParser.ElementContext): Element
    {
      val identifier = visitResolvedIdentifier(ctx.resolvedIdentifier())
      val extends = if (ctx.extends_() == null)
        ArrayList<ResolvedIdentifier>()
      else
        ctx.extends_().resolvedIdentifier().map { visitResolvedIdentifier(it)}
      val body = visitBody(ctx.body())
      val location = symbolToLocation(ctx.start)
      return when
      {
        ctx.type().INPUT() != null -> Input(location, identifier, extends, body)
        ctx.type().INTERFACE() != null -> Interface(location, identifier, extends, body)
        ctx.type().OUTPUT() != null -> Output(location, identifier, extends, body)
        ctx.type().OBJECT() != null -> Object(location, identifier, extends, body)
        else -> throw IllegalStateException("Found element $ctx with unknown/unhandled type")
      }
    }

    override fun visitResolvedIdentifier(ctx: GlidingSoarParser.ResolvedIdentifierContext): ResolvedIdentifier
    {
      val namespace = ctx.IDENTIFIER().subList(0, ctx.IDENTIFIER().size - 1).map { convertIdentifier(it) }
      return ResolvedIdentifier(symbolToLocation(ctx.start), namespace, ctx.IDENTIFIER().last().text)
    }

    override fun visitBody(ctx: GlidingSoarParser.BodyContext): Body
    {
      val parameters = ctx.bodyElement().filter { it.parameter() != null }.map { visitParameter(it.parameter()) }
      val members = ctx.bodyElement().filter { it.member() != null }.map { visitMember(it.member()) }
      val matches = ctx.bodyElement().filter { it.match() != null }.map { visitMatch(it.match()) }
      return Body(symbolToLocation(ctx.OPEN_CURLY().symbol), parameters, members, matches)
    }

    override fun visitParameter(ctx: GlidingSoarParser.ParameterContext): Parameter
    {
      val identifier = convertIdentifier(ctx.IDENTIFIER(0))
      val type = convertIdentifier(ctx.IDENTIFIER(1))
      val multiple = ctx.MULTIPLE() != null
      val optional = ctx.OPTIONAL() != null
      return Parameter(symbolToLocation(ctx.start), identifier, type, multiple, optional)
    }

    override fun visitMember(ctx: GlidingSoarParser.MemberContext): Member
    {
      val identifier = convertIdentifier(ctx.IDENTIFIER(0))
      val type = convertIdentifier(ctx.IDENTIFIER(1))
      val tag = ctx.TAG() != null
      val multiple = ctx.MULTIPLE() != null
      if (ctx.I_SUPPORT() != null && ctx.O_SUPPORT() != null)
        throw IllegalStateException("Found member that is both o support and i support!: $ctx")
      return when
      {
        ctx.I_SUPPORT() != null -> IMember(symbolToLocation(ctx.start), tag, identifier, type, multiple)
        ctx.O_SUPPORT() != null -> OMember(symbolToLocation(ctx.start), tag, identifier, type, multiple)
        else -> throw IllegalStateException("Found member that is neither i support nor o support!: $ctx")
      }
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

  fun symbolToLocation(symbol: Token) = Location(source.line + symbol.line, symbol.charPositionInLine)
}
