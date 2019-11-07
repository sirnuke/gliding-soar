import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
  base
  kotlin("jvm") version "1.3.50" apply false
  java
}

allprojects {
  group = "com.degrendel"
  version = "1.0-SNAPSHOT"

  repositories {
    mavenCentral()
    jcenter()
  }
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "org.jetbrains.kotlin.jvm")

  dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.slf4j:slf4j-simple:1.7.25")
  }
}
