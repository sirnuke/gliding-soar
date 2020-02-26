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
          Arguments.of("input Simple {}", Element.ElementType.INPUT),
          Arguments.of("output Simple {}", Element.ElementType.OUTPUT),
          Arguments.of("object Simple {}", Element.ElementType.OBJECT),
          Arguments.of("interface Simple {}", Element.ElementType.INTERFACE)
      )
    }
  }

  private val parser = BlockParser()
  private val location = Location("\$junit", 1, 0)

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

  private fun assertMember(member: Member, name: String, type: String, support: Member.SupportType = Member.SupportType.UNRESTRICTED, optional: Boolean = false, const: Boolean = false, tag: Boolean = false, multiple: Boolean = false)
  {
    assertEquals(name, member.identifier.value)
    assertEquals(type, member.type.value)
    assertEquals(support, member.support)
    assertEquals(tag, member.tag)
    assertEquals(multiple, member.multiple)
    assertEquals(const, member.const)
    assertEquals(optional, member.optional)
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
  fun `Parses a simple elements`(block: String, type: Element.ElementType)
  {
    val elements = assertSuccess(block).elements
    assertEquals(1, elements.size)
    val element = elements.first()
    assertEquals(type, element.type)
    assertTrue(element.extends.isEmpty())
    assertEquals("Simple", element.identifier.value)
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
    assertEquals(name, element.identifier.value)
  }


  @Test
  fun `Parses object parameters`()
  {
    val block = """
      object FooBar {
        foo: Boolean
        bar: FooBar?
        hello: Int!
        world: Thing+
        what: String+?
        hmm: Nope+!
        asdf: ASDF!?
        backwards: Forwards+!?
      }
    """.trimIndent()
    val element = assertSuccess(block).elements.first()
    assertEquals(8, element.members.size)
    assertMember(element.members[0], "foo", "Boolean")
    assertMember(element.members[1], "bar", "FooBar", optional = true)
    assertMember(element.members[2], "hello", "Int", const = true)
    assertMember(element.members[3], "world", "Thing", multiple = true)
    assertMember(element.members[4], "what", "String", optional = true, multiple = true)
    assertMember(element.members[5], "hmm", "Nope", const = true, multiple = true)
    assertMember(element.members[6], "asdf", "ASDF", const = true, optional = true)
    assertMember(element.members[7], "backwards", "Forwards", optional = true, const = true, multiple = true)
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
    val element = assertSuccess(block).elements.first()
    assertEquals(4, element.members.size)
    assertMember(element.members[0], "foo", "String", support = Member.SupportType.ISUPPORT)
    assertMember(element.members[1], "bar", "Thing", support = Member.SupportType.OSUPPORT)
    assertMember(element.members[2], "hello", "Int", multiple = true, support = Member.SupportType.ISUPPORT)
    assertMember(element.members[3], "world", "Tag", tag = true, support = Member.SupportType.OSUPPORT)
  }
}