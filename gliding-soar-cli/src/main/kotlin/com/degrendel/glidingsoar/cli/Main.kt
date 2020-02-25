package com.degrendel.glidingsoar.cli

import com.degrendel.glidingsoar.common.Version
import com.degrendel.glidingsoar.common.logger
import com.degrendel.glidingsoar.service.ModelImpl
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(name = "GlidingSoar", mixinStandardHelpOptions = true)
class Main(private val args: Array<String>) : Callable<Int>
{
  companion object
  {
    val L by logger()
  }

  @Option(names = ["--soar-file-extensions"], description = ["Treat files with the following extensions as Soar source code, and scan for embedded Glide definitions; separated by commas"], defaultValue = "soar,tcl", split = ",")
  private var soarFileExtensions: List<String> = mutableListOf()

  @Option(names = ["--glide-file-extensions"], description = ["Treat files with the following extensions as containing raw Glide definitions; separated by commas"], defaultValue = "glide", split = ",")
  private var glideFileExtensions: List<String> = mutableListOf()

  @Option(names = ["--ignore-hidden-files", "-i"], description = ["Ignore hidden files and directories, i.e. starts with period on UNIX or marked hidden on Windows"])
  private var ignoreHiddenFiles = true

  @Option(names = ["--recursive", "-r"], description = ["Recurse into subdirectories"])
  private var recursiveMode = true

  @Option(names = ["--output", "-o"], description = ["Save generated TCL code to this file, default is dump to standard output"])
  private var outputFile: File? = null

  @Parameters(paramLabel = "target", description = ["List of files to parse or directories to search; files are assumed to be Glide"], arity = "1..*")
  private var targets: List<File> = mutableListOf()

  @Option(names = ["--standalone", "-s"], description = ["Whether this a standalone bundle (aka should the common functions be defined?)"])
  private var standalone = true

  @Option(names = ["--continuous", "-c"], description = ["Whether to continually scan the files and recompile as needed"])
  private var continuous = false

  // NOTE: Ideally would use Files.isSameFile, but that throws an exception if the file is missing
  // Java! \o/
  private fun isOutput(outputFile: URI?, file: File): Boolean =
    (outputFile == file.toPath().toAbsolutePath().normalize().toUri())

  override fun call(): Int
  {
    L.info("Running Glide CLI version {}", Version.VERSION)
    val outputUri = outputFile?.toPath()?.toAbsolutePath()?.normalize()?.toUri()
    process(outputUri)
    if (continuous)
    {
      L.info("Entering continuous mode")
      val watcher = FileSystems.getDefault().newWatchService()
      val keys = mutableMapOf<WatchKey, File>()
      val dirs = mutableSetOf<File>()
      val register = { dir: File ->
        if (dirs.contains(dir))
          L.info("Already watching {}", dir)
        else
        {
          val key = dir.toPath().register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
          keys[key] = dir
        }
      }
      targets.forEach {
        if (!it.exists())
          throw FileNotFoundException("Target $it does not exist!")
        else if (it.isDirectory)
        {
          register(it)
          if (recursiveMode)
            it.walk().asSequence().filter { f -> f.isDirectory }.forEach { f -> register(f) }
        }
        else if (it.isFile)
          register(it.parentFile)
        else
          throw IllegalStateException("Target $it that exists but isn't directory or file?")
      }

      while (true)
      {
        val key = watcher.take()
        val file = keys.getValue(key)
        key.pollEvents().forEach {
          // TODO: If subdirs are created/deleted, refresh the watcher
          when (it.kind())
          {
            ENTRY_CREATE -> L.info("File creation detected!: {}", it)
            ENTRY_MODIFY -> L.info("File modified detected!: {}", it)
            ENTRY_DELETE -> L.info("File deletion detected!: {}", it)
            OVERFLOW -> L.warn("Overflow event detected?: {}", it)
          }
        }
        L.info("Detected modification of {}, rebuilding!", file)
        process(outputUri)
      }
    }
    return 0
  }

  private fun walkDirectory(outputUri: URI?, target: File, lambda: (TargetType, File) -> Unit)
  {
    val ignoreHidden = ignoreHiddenFiles
    val iterator = if (recursiveMode)
      target.walk().asSequence()
    else
      target.listFiles()!!.asSequence()
    val files = iterator.filter { f -> f.isFile }
        .filter { f -> !ignoreHidden || !f.isHidden }
        .filterNot { f -> isOutput(outputUri, f) }
    files.filter { f -> soarFileExtensions.contains(f.extension.toLowerCase()) }
        .forEach { f -> lambda(TargetType.SOAR, f) }
    files.filter { f -> glideFileExtensions.contains(f.extension.toLowerCase()) }
        .forEach { f -> lambda(TargetType.GLIDE, f) }
  }

  private fun process(outputUri: URI?)
  {
    val model = ModelImpl(args, standalone)
    targets.forEach {
      if (isOutput(outputUri, it))
        throw IllegalArgumentException("Target $it matches the output file!")
      else if (!it.exists())
        throw FileNotFoundException("Target $it does not exist!")
      else if (it.isDirectory)
      {
        walkDirectory(outputUri, it) { type, file ->
          when (type)
          {
            TargetType.SOAR -> model.parseSoarFile(file.toURI())
            TargetType.GLIDE -> model.parseGlideFile(file.toURI())
          }
        }
      }
      else if (it.isFile)
      {
        if (soarFileExtensions.contains(it.extension.toLowerCase()))
          model.parseSoarFile(it.toURI())
        else
        {
          if (!glideFileExtensions.contains(it.extension.toLowerCase()))
            L.warn("Target file {} doesn't have Soar or Glide extension, assuming glide", it)
          model.parseGlideFile(it.toURI())
        }
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
  }
}

enum class TargetType
{
  SOAR, GLIDE, ;
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(Main(args)).execute(*args))
