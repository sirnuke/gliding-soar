package com.degrendel.glidingsoar.cli

import com.degrendel.glidingsoar.common.logger
import picocli.CommandLine
import picocli.CommandLine.Command
import kotlin.system.exitProcess

@Command(name = "GlidingSoar", mixinStandardHelpOptions = true)
class Main
{
  companion object
  {
    val L by logger()
  }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(Main()).execute(*args))
