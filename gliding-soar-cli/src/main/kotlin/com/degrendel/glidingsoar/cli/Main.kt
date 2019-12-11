package com.degrendel.glidingsoar.cli

import com.degrendel.glidingsoar.common.Version
import com.degrendel.glidingsoar.common.logger
import com.degrendel.glidingsoar.service.ModelImpl
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(name = "GlidingSoar", mixinStandardHelpOptions = true)
class Main(private val args: Array<String>) : Callable<Int>
{
  companion object
  {
    val L by logger()
  }

  @Option(names = ["--file-extensions"], description = ["Limit discovered files to the following extensions, separated by commas"], defaultValue = "soar,tcl", split = ",")
  private var fileExtensions: List<File> = mutableListOf()

  @Option(names = ["--ignore-hidden-files", "-i"], description = ["Ignore hidden files and directories, i.e. any that start with a period"])
  private var ignoreHiddenFiles = true

  @Option(names = ["--recursive", "-r"], description = ["Recurse into subdirectories"])
  private var recursiveMode = true

  @Option(names = ["--output", "-o"], description = ["Save generated TCL code to this file, default is dump to standard output"])
  private var outputFile: File? = null

  @Parameters(paramLabel = "target", description = ["List of files to parse or directories to search"], arity = "1..*")
  private var targets: List<File> = mutableListOf()

  override fun call(): Int
  {
    L.info("Running Glide CLI version {}", Version.VERSION)
    val model = ModelImpl(args)
    targets.forEach {
      if (!it.exists())
      {
        L.error("Target {} does not exist!", it)
        return -1
      }
      else if (it.isDirectory)
      {
        // Step 1: glob all with extensions in fileExtensions, ignore hidden if ignoreHiddenFiles
        // Step 2: if recursiveMode, recursive into all directories, ignore hidden if ignoredHiddenFiles
        TODO("Directories are not yet implemented!")
      }
      else if (it.isFile)
      {
        model.parseFile(it.toURI())
      }
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
