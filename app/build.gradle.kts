import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.infinitysandbox.wkqz"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    val envProps = Properties()
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
      envFile.inputStream().use { stream ->
        envProps.load(stream)
      }
    }
    fun getEnvProp(key: String, fallback: String): String {
      val raw = envProps.getProperty(key)?.trim()?.removeSurrounding("\"")
      return if (!raw.isNullOrBlank()) raw else fallback
    }

    buildConfigField("String", "GEMINI_API_KEY", "\"${getEnvProp("GEMINI_API_KEY", "")}\"")
    buildConfigField("String", "NV_KEY1", "\"${getEnvProp("NV_KEY1", "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")}\"")
    buildConfigField("String", "NV_KEY2", "\"${getEnvProp("NV_KEY2", "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")}\"")
    buildConfigField("String", "NV_KEY3", "\"${getEnvProp("NV_KEY3", "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")}\"")
    buildConfigField("String", "NV_KEY4", "\"${getEnvProp("NV_KEY4", "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")}\"")
    buildConfigField("String", "NV_KEY5", "\"${getEnvProp("NV_KEY5", "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")}\"")
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  ignoreList.add("GEMINI_API_KEY")
  ignoreList.add("NV_KEY1")
  ignoreList.add("NV_KEY2")
  ignoreList.add("NV_KEY3")
  ignoreList.add("NV_KEY4")
  ignoreList.add("NV_KEY5")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Dependencies for Infinity Sandbox multi-agent architecture
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
