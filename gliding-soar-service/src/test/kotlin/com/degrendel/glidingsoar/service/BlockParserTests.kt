package com.degrendel.glidingsoar.service

import com.degrendel.glidingsoar.common.ast.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
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

  private fun assertFailure(block: String): ParseFailure
  {
    return when (val result = parser.parse(location, block))
    {
      is ParseFailure -> result
      is ParseSuccess -> fail("Expected parse failure!")
    }
  }

  private fun assertParameter(param: Parameter, name: String, type: String, optional: Boolean = false, multiple: Boolean = false)
  {
    assertEquals(name, param.identifier.value)
    assertEquals(type, param.type.value)
    assertEquals(optional, param.optional)
    assertEquals(multiple, param.multiple)
  }

  private fun assertMember(member: Member, name: String, type: String, i_support: Boolean = true, tag: Boolean = false, multiple: Boolean = false)
  {
    assertEquals(name, member.identifier.value)
    assertEquals(type, member.type.value)
    val klass = if (i_support) IMember::class else OMember::class
    assertEquals(klass, member::class)
    assertEquals(tag, member.tag)
    assertEquals(multiple, member.multiple)
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

  @ParameterizedTest
  @ValueSource(strings = ["hello", "HELLO", "hel*oo", "-", "*", "-*-*-*---*"])
  fun `Parses various method names`(name: String)
  {
    val block = """
      object $name {
      }
    """.trimIndent()
    val element = assertSuccess(block).elements.first()
    assertEquals(name, element.declaration.identifier.value)
  }


  @Test
  fun `Parses object parameters`()
  {
    val block = """
      object FooBar {
        param foo: Boolean
        param bar: FooBar
        param hello: Int?
        param world: Thing+
        param what: String+?
      }
    """.trimIndent()
    val body = assertSuccess(block).elements.first().body
    assertEquals(5, body.parameters.size)
    assertParameter(body.parameters[0], "foo", "Boolean")
    assertParameter(body.parameters[1], "bar", "FooBar")
    assertParameter(body.parameters[2], "hello", "Int", optional = true)
    assertParameter(body.parameters[3], "world", "Thing", multiple = true)
    assertParameter(body.parameters[4], "what", "String", optional = true, multiple = true)
  }

  @Test
  fun `Parses object members`()
  {
    val block = """
      object FooBar {
        i foo: String
        o bar: Thing
        i hello: Int+
        o tag world: Tag
      }
    """.trimIndent()
    val body = assertSuccess(block).elements.first().body
    assertEquals(4, body.members.size)
    assertMember(body.members[0], "foo", "String")
    assertMember(body.members[1], "bar", "Thing", i_support = false)
    assertMember(body.members[2], "hello", "Int", multiple = true)
    assertMember(body.members[3], "world", "Tag", i_support = false, tag = true)
  }
}