plugins {
  id("com.android.application")
  id("org.lsposed.lsparanoid")
  id("android-base-conventions")
}

android {
  compileSdk = rootProject.getProjIntExtra("android.sdk.compile")
  buildToolsVersion = rootProject.getProjStringExtra("android.sdk.tools.build")

  defaultConfig {
    minSdk = rootProject.getProjIntExtra("android.sdk.min")
    targetSdk = rootProject.getProjIntExtra("android.sdk.target")

    multiDexEnabled = true // Required for desugaring
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    isCoreLibraryDesugaringEnabled = true
  }

  kotlinOptions.jvmTarget = "17"

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true

      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  dependenciesInfo.includeInApk = false

  buildFeatures {
    viewBinding = true
    dataBinding = true
    buildConfig = true
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.compose.kotlin.compiler.ext.get()
  }

  packaging {
    // https://github.com/Kotlin/kotlinx.coroutines#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
    resources.excludes += "DebugProbesKt.bin"
  }
}

lsparanoid {
  includeDependencies = true
  classFilter = { it.startsWith("com.mirfatif.") }
}

dependencies {
  implementation(libs.androidx.annotation)
  implementation(libs.kotlinx.coroutines.android)
  coreLibraryDesugaring(libs.desugar.jdk)
}
