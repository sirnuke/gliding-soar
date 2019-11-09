val antlrVersion = "4.7.2"

plugins {
  antlr
}

tasks.compileKotlin {
  dependsOn.add(tasks.generateGrammarSource)
}

tasks.generateGrammarSource {
  arguments.addAll(listOf("-visitor", "-long-messages", "-no-listener", "-package", "com.degrendel.glidingsoar.service.grammar"))
  outputDirectory = file("$buildDir/generated-src/antlr/main/com/degrendel/glidingsoar/service/grammar")
}

dependencies {
  api(project(":gliding-soar-common"))
  implementation("org.antlr:antlr4-runtime:$antlrVersion")
  antlr("org.antlr:antlr4:$antlrVersion")
}