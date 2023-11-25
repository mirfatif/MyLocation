plugins {
  id("java-library")
  id("java-lib-conventions")
}

dependencies { compileOnly(libs.lsparanoid.core) }
