plugins { id("com.android.application") version "8.12.3" }

android {
  namespace = "com.mirfatif.mylocation"

  compileSdk = 36
  buildToolsVersion = "36.0.0"

  defaultConfig {
    applicationId = namespace
    minSdk = 21
    targetSdk = 36
    versionCode = 108
    versionName = "v1.08"
    versionNameSuffix = "-foss"
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }

    release {
      isMinifyEnabled = true
      isShrinkResources = true
      @Suppress("UnstableApiUsage")
      postprocessing.isObfuscate = false

      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
  }

  buildFeatures {
    buildConfig = true
    aidl = true
    viewBinding = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation("androidx.appcompat:appcompat:1.7.1")
  implementation("androidx.recyclerview:recyclerview:1.4.0")
  implementation("androidx.browser:browser:1.9.0")
  implementation("com.google.android.material:material:1.14.0-alpha08")
  implementation("me.saket:better-link-movement-method:2.2.0")
}
