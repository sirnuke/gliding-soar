plugins {
  application
}

application {
  mainClassName = "com.degrendel.glidingsoar.cli.MainKt"
}

dependencies {
  implementation("info.picocli:picocli:4.1.1")
  implementation(project(":gliding-soar-service"))
}
