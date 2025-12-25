fun RepositoryHandler.repos() {
  mavenCentral()
  google()
  gradlePluginPortal()
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories.repos()
}

pluginManagement.repositories.repos()

include(":app")
