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
      val declaration = visitDeclaration(ctx.declaration())
      val body = visitBody(ctx.body())
      return Element(symbolToLocation(ctx.start), declaration, body)
    }

    override fun visitDeclaration(ctx: GlidingSoarParser.DeclarationContext): Declaration
    {
      val identifier = convertIdentifier(ctx.IDENTIFIER())
      val type = visitType(ctx.type())
      val extends = if (ctx.extends_() == null)
        ArrayList<Identifier>()
      else
        ctx.extends_().IDENTIFIER().map { convertIdentifier(it) }
      return Declaration(symbolToLocation(ctx.start), type, identifier, extends)
    }

    override fun visitType(ctx: GlidingSoarParser.TypeContext): Type
    {
      return when
      {
        ctx.INPUT() != null -> Input(symbolToLocation(ctx.INPUT().symbol))
        ctx.OBJECT() != null -> Object(symbolToLocation(ctx.OBJECT().symbol))
        ctx.OUTPUT() != null -> Output(symbolToLocation(ctx.OUTPUT().symbol))
        ctx.INTERFACE() != null -> Interface(symbolToLocation(ctx.INTERFACE().symbol))
        else -> throw IllegalStateException("Found type $ctx that isn't input/object/output/interface")
      }
    }

    override fun visitBody(ctx: GlidingSoarParser.BodyContext): Body
    {
      val members = ctx.bodyElement().filter { it.members() != null }.map { it.members().member().map { member -> visitMember(member) } }.flatten()
      val tags = ctx.bodyElement().filter { it.tags() != null }.map { it.tags().tag().map { tag -> visitTag(tag) } }.flatten()
      val matches = ctx.bodyElement().filter { it.matches() != null }.map { it.matches().match().map { match -> visitMatch(match) } }.flatten()
      return Body(symbolToLocation(ctx.CLOSE_CURLY().symbol), members, tags, matches)
    }

    // TODO: Actually parse member/tag/match values
    override fun visitMember(ctx: GlidingSoarParser.MemberContext): Member
    {
      return Member(symbolToLocation(ctx.start))
    }

    override fun visitTag(ctx: GlidingSoarParser.TagContext): Tag
    {
      return Tag(symbolToLocation(ctx.start))
    }

    override fun visitMatch(ctx: GlidingSoarParser.MatchContext): Match
    {
      return Match(symbolToLocation(ctx.start))
    }

    fun convertIdentifier(identifier: TerminalNode) = Identifier(symbolToLocation(identifier.symbol), identifier.symbol.text)
  }


  fun symbolToLocation(symbol: Token) = Location(source.line + symbol.line, symbol.charPositionInLine)
}
