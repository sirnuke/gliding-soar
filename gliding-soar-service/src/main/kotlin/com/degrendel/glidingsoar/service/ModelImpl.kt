package com.degrendel.glidingsoar.service

import com.degrendel.glidingsoar.common.*
import com.degrendel.glidingsoar.common.ast.*
import org.stringtemplate.v4.STGroupFile
import java.io.File
import java.net.URI
import java.time.Instant

class ModelImpl(private val arguments: Array<String>?) : Model
{
  companion object
  {
    val L by logger()
  }

  private val root = RootNamespace()
  private val elements = mutableListOf<Element>()
  private val template = STGroupFile(javaClass.getResource("/templates/bundle.stg"))

  override fun bundle(): String
  {
    L.info("Generating bundle")
    root.validate()
    val bundle = template.getInstanceOf("bundle")
    bundle.add("model", this)
    bundle.add("version", Version.VERSION)
    bundle.add("when", Instant.now())
    bundle.add("arguments", arguments)
    bundle.add("root", root)
    return bundle.render()
  }

  override fun parseFile(uri: URI)
  {
    L.info("Parsing URI {}", uri)
    parseContents(uri.path, File(uri.path).readText())
  }

  override fun parseString(source: String, contents: String)
  {
    L.info("Parsing string from {} of length {}", source, contents.length)
    parseContents(source, contents)
  }

  private fun parseContents(source: String, contents: String)
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
    val parser = BlockParser()
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
