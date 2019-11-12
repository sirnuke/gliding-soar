package com.degrendel.glidingsoar.service

import com.degrendel.glidingsoar.common.ast.Element
import com.degrendel.glidingsoar.common.ast.Location

fun parseString(contents: String): ParserResults
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
  val parser = ElementParser()
  contents.lines().forEachIndexed { index, line ->
    when (inBody)
    {
      false -> if (start.matches(line)) inBody = true
      true ->
      {
        if (stop.matches(line))
        {
          inBody = false
          when (val result = parser.parse(Location(index, 0), body.joinToString(separator = "\n")))
          {
            is ParseSuccess -> results.addAll(result.elements)
            is ParseFailure -> return result
          }
        }
        else
        {
          val inner = content.find(line)
          if (inner != null)
            body.add(inner.groups["content"]!!.value)
          else
            return ParseFailure(Location(index + 1, 0), "Unexpected line, expected line of content or </glide>")
        }
      }
    }
  }
  return ParseSuccess(results)
}
