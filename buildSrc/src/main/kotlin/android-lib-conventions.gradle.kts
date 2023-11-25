plugins {
  id("com.android.library")
  id("android-base-conventions")
}

android {
  compileSdk = rootProject.getProjIntExtra("android.sdk.compile")
  buildToolsVersion = rootProject.getProjStringExtra("android.sdk.tools.build")

  defaultConfig.minSdk = rootProject.getProjIntExtra("android.sdk.min")

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions.jvmTarget = "17"

  buildFeatures.buildConfig = false
}

dependencies {
  implementation(libs.androidx.annotation)
  implementation(libs.kotlinx.coroutines.android)
  compileOnly(libs.lsparanoid.core)
}
