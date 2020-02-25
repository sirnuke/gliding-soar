package com.degrendel.glidingsoar.service

import com.degrendel.glidingsoar.common.*
import com.degrendel.glidingsoar.common.ast.*
import org.stringtemplate.v4.AttributeRenderer
import org.stringtemplate.v4.STGroupFile
import java.io.File
import java.net.URI
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

class ModelImpl(private val arguments: Array<String>?, private val standalone: Boolean) : Model
{
  companion object
  {
    val L by logger()
  }

  private val root = RootNamespace()
  private val elements = mutableListOf<Element>()
  private val template = STGroupFile(javaClass.getResource("/templates/bundle.stg"))
  private val parser = BlockParser()

  private object ElementRenderer : AttributeRenderer
  {
    private val outputTemplate = STGroupFile(javaClass.getResource("/templates/output.stg"))
    private val output = outputTemplate.getInstanceOf("output")
    private val inputTemplate = STGroupFile(javaClass.getResource("/templates/input.stg"))
    private val input = inputTemplate.getInstanceOf("input")

    override fun toString(obj: Any, formatString: String?, locale: Locale?): String
    {
      if (obj !is Element)
        throw IllegalArgumentException("ElementRenderer recieved unexpected non-Element ${obj::class.java}: $obj")
      L.info("Converting element {} of type {} to a string", obj, obj.type)
      val renderer = when (obj.type)
      {
        Element.ElementType.OUTPUT -> output
        Element.ElementType.INPUT -> input
        Element.ElementType.OBJECT -> TODO("Objects aren't implemented!")
        Element.ElementType.INTERFACE -> return ""
      }
      renderer.add("element", obj)
      return renderer.render()
    }
  }

  init
  {
    template.registerRenderer(Element::class.java, ElementRenderer)
  }

  override fun bundle(): String
  {
    L.info("Generating bundle")
    val bundle = template.getInstanceOf("bundle")
    root.resolve()
    bundle.add("model", this)
    bundle.add("version", Version.VERSION)
    bundle.add("when", Instant.now())
    bundle.add("arguments", arguments)
    bundle.add("standalone", standalone)
    bundle.add("root", root)
    return bundle.render()
  }

  override fun parseSoarFile(uri: URI)
  {
    L.info("Parsing Soar from URI {}", uri)
    parseInlineContents(uri.path, File(uri.path).readText())
  }

  override fun parseSoarString(source: String, contents: String)
  {
    L.info("Parsing Soar from string {} of length {}", source, contents.length)
    parseInlineContents(source, contents)
  }

  override fun parseGlideFile(uri: URI)
  {
    L.info("Parsing Glide from URI {}", uri)
    parseRawContents(uri.path, File(uri).readText())
  }

  override fun parseGlideString(source: String, contents: String)
  {
    L.info("Parsing Glide from string {} of length {}", source, contents.length)
    parseRawContents(source, contents)
  }

  private fun parseRawContents(source: String, contents: String)
  {
    val results: List<Element>
    when (val result = parser.parse(Location(source, 0, 0), contents))
    {
      is ParseSuccess -> results = result.elements
      is ParseFailure -> throw GlideParseException(result.toHumanString())
    }
    L.info("Found {} elements", results.size)
    elements.addAll(results)
    root.addElements(results)
  }

  private fun parseInlineContents(source: String, contents: String)
  {
    // TODO: This is kinda painful
    // Being able to filter content on a start/stop predicate feels like it should be possible, but maybe not in Kotlin?
    // Could probably also adapt the grammar to ignore non comments and whatnot
    // Could also also turn this into a single monster multiline regex but Regex: now you have two problems
    val start = Regex("^\\s*#\\s*<glide>\\s*$")
    val content = Regex("^\\s*#\\s*(?<content>.*)$")
    val stop = Regex("^\\s*#\\s*</glide>\\s*$")
    val results = ArrayList<Element>()
    val body = ArrayList<String>()
    var inBody = false
    contents.lines().forEachIndexed { index, line ->
      when (inBody)
      {
        false -> if (start.matches(line)) inBody = true
        true ->
        {
          if (stop.matches(line))
          {
            inBody = false
            when (val result = parser.parse(Location(source, index, 0), body.joinToString(separator = "\n")))
            {
              is ParseSuccess -> results.addAll(result.elements)
              is ParseFailure -> throw GlideParseException(result.toHumanString())
            }
          }
          else
          {
            val inner = content.find(line)
            if (inner != null)
              body.add(inner.groups["content"]!!.value)
            else
              throw GlideParseException(ParseFailure(Location(source, index + 1, 0), "Unexpected line, expected line of content or </glide>").toHumanString())
          }
        }
      }
    }
    if (inBody)
      throw GlideParseException(ParseFailure(Location(source, contents.lines().size, 0), "Unterminated glide block, expected </glide>").toHumanString())
    L.info("Found {} elements", results.size)
    elements.addAll(results)
    root.addElements(results)
  }
}
