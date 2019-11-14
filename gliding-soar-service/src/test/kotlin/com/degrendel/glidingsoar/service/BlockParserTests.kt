package com.degrendel.glidingsoar.service

import com.degrendel.glidingsoar.common.ast.Location
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BlockParserTests
{
  private val parser = BlockParser()
  private val location = Location(1, 0)

  @ParameterizedTest
  @ValueSource(strings = ["", "\t", "\n\n", "  \n\t  \n\n", "# comment!\n", "// alt comment!", " # another", "\t//yeah"])
  fun `Various no element strings parse and result in zero elements`(block: String)
  {
    val result = parser.parse(location, block)
    if (result is ParseSuccess)
      assertTrue(result.elements.isEmpty())
    else
      fail("Parsing failed!")
  }
}