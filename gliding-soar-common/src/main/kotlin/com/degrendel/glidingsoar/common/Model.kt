package com.degrendel.glidingsoar.common

import java.net.URI

interface Model
{
  fun bundle(): String
  fun parseFile(uri: URI)
  fun parseString(source: String, contents: String)
}

class GlideParseException(message: String): Exception(message)