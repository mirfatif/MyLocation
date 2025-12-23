plugins { id("com.diffplug.spotless") version "7.2.1" }

subprojects { plugins.apply("com.diffplug.spotless") }

allprojects {
  spotless {
    java {
      target("**/*.java")
      googleJavaFormat("1.28.0")
    }

    kotlin {
      target("**/*.kt", "**/*.kts")
      ktfmt("0.56")
    }
  }

  tasks.withType<JavaCompile> { options.compilerArgs.add("-Xlint:all") }
}
