package com.degrendel.glidingsoar.cli

import com.degrendel.glidingsoar.common.Version
import com.degrendel.glidingsoar.common.logger
import com.degrendel.glidingsoar.service.ModelImpl
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(name = "GlidingSoar", mixinStandardHelpOptions = true)
class Main(private val args: Array<String>) : Callable<Int>
{
  companion object
  {
    val L by logger()
  }

  @Option(names = ["--file-extensions"], description = ["Limit discovered files to the following extensions, separated by commas"], defaultValue = "soar,tcl,glide", split = ",")
  private var fileExtensions: List<String> = mutableListOf()

  @Option(names = ["--ignore-hidden-files", "-i"], description = ["Ignore hidden files and directories, i.e. starts with period on UNIX or marked hidden on Windows"])
  private var ignoreHiddenFiles = true

  @Option(names = ["--recursive", "-r"], description = ["Recurse into subdirectories"])
  private var recursiveMode = true

  @Option(names = ["--output", "-o"], description = ["Save generated TCL code to this file, default is dump to standard output"])
  private var outputFile: File? = null

  @Parameters(paramLabel = "target", description = ["List of files to parse or directories to search"], arity = "1..*")
  private var targets: List<File> = mutableListOf()

  @Option(names = ["--standalone", "-s"], description= ["Whether this a standalone bundle (aka should the common functions be defined?)"])
  private var standalone = true

  // NOTE: Ideally would use Files.isSameFile, but that throws an exception if the file is missing
  // Java! \o/
  private fun isOutput(outputFile: URI?, file: File): Boolean =
    (outputFile == file.toPath().toAbsolutePath().normalize().toUri())

  override fun call(): Int
  {
    L.info("Running Glide CLI version {}", Version.VERSION)
    val outputUri = outputFile?.toPath()?.toAbsolutePath()?.normalize()?.toUri()
    val model = ModelImpl(args, standalone)
    targets.forEach {
      if (isOutput(outputUri, it))
      {
        L.error("Target {} matches the output file!", it)
        return -1
      }
      else if (!it.exists())
      {
        L.error("Target {} does not exist!", it)
        return -1
      }
      else if (it.isDirectory)
      {
        val ignoreHidden = ignoreHiddenFiles
        val iterator = if (recursiveMode)
          it.walk().asSequence()
        else
          it.listFiles()!!.asSequence()
        iterator.filter { f -> f.isFile }
            .filter { f -> !ignoreHidden || !f.isHidden }
            .filterNot { f -> isOutput(outputUri, f) }
            .filter { f -> fileExtensions.isEmpty() || fileExtensions.contains(f.extension.toLowerCase()) }
            .forEach { f -> model.parseFile(f.toURI()) }
      }
      else if (it.isFile)
        model.parseFile(it.toURI())
      else
        throw IllegalStateException("Received target $it that exists() but is not isDirectory nor isFile (?)")
    }
    val bundle = model.bundle()
    val outputFileReference = outputFile
    if (outputFileReference != null)
      outputFileReference.writeText(bundle)
    else
      println(bundle)
    return 0
  }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(Main(args)).execute(*args))
