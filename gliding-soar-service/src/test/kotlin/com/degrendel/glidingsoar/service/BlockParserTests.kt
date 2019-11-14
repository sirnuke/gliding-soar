package com.degrendel.glidingsoar.service

import com.degrendel.glidingsoar.common.ast.Input
import com.degrendel.glidingsoar.common.ast.Interface
import com.degrendel.glidingsoar.common.ast.Location
import com.degrendel.glidingsoar.common.ast.Output
import com.degrendel.glidingsoar.common.ast.Object
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream
import kotlin.reflect.KClass

class BlockParserTests
{
  companion object
  {
    // Gross status: kinda gross
    @JvmStatic
    fun simpleObjects(): Stream<Arguments>
    {
      return Stream.of(
          Arguments.of("input Simple {}", Input::class),
          Arguments.of("output Simple {}", Output::class),
          Arguments.of("object Simple {}", Object::class),
          Arguments.of("interface Simple {}", Interface::class)
      )
    }
  }

  private val parser = BlockParser()
  private val location = Location(1, 0)

  private fun assertSuccess(block: String): ParseSuccess
  {
    return when (val result = parser.parse(location, block))
    {
      is ParseSuccess -> result
      is ParseFailure -> fail("Unexpected parse failure!")
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["", "\t", "\n\n", "  \n\t  \n\n", "# comment!\n", "// alt comment!", " # another", "\t//yeah"])
  fun `Various no element strings parse and result in zero elements`(block: String)
  {
    val result = assertSuccess(block)
    assertTrue(result.elements.isEmpty())
  }

  @ParameterizedTest
  @MethodSource("simpleObjects")
  fun `Parses a simple elements`(block: String, type: KClass<Any>)
  {
    val elements = assertSuccess(block).elements
    assertEquals(1, elements.size)
    val element = elements.first()
    assertEquals(type, element.declaration.type::class)
    assertTrue(element.declaration.extends.isEmpty())
    assertEquals("Simple", element.declaration.identifier.value)
  }
}