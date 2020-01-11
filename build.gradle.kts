import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  base
  kotlin("jvm") version "1.3.50" apply false
  java
  `maven-publish`
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

tasks.register("printVersion") {
  doLast {
    println(project.version)
  }
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "maven-publish")

  val internalNexusUsername: String by project
  val internalNexusPassword: String by project
  val internalNexusURL: String by project

  publishing {
    publications {
      create<MavenPublication>("maven") {
        from(components["java"])
      }
    }
    repositories {
      maven {
        credentials {
          username = internalNexusUsername
          password = internalNexusPassword
        }
        val releasesRepoUrl = "$internalNexusURL/repository/maven-releases/"
        val snapshotsRepoUrl = "$internalNexusURL/repository/maven-snapshots/"
        url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        name = "Internal Nexus"
      }
    }
  }

  repositories {
    maven {
      credentials {
        username = internalNexusUsername
        password = internalNexusPassword
      }
      url = uri("$internalNexusURL/repository/maven-public")
      name = "Internal Nexus"
    }
  }


  dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.slf4j:slf4j-simple:1.7.25")
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }
}
