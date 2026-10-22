import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.cinestreamadmin.adcxz"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

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
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
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
  ignoreList.add("MAPS_API_KEY")
}

// Ensure debug.keystore exists with canonical fingerprint (cinestream-admin)
val debugKeystoreFile = file("${rootDir}/debug.keystore")
if (!debugKeystoreFile.exists()) {
  val base64Keystore = "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"
  debugKeystoreFile.writeBytes(Base64.getDecoder().decode(base64Keystore))
}

// Ensure google-services.json exists so Google Services plugin and builds always succeed
val googleServicesFile = file("google-services.json")
if (!googleServicesFile.exists()) {
  googleServicesFile.writeText(
    """
    {
      "project_info": {
        "project_number": "979447256418",
        "firebase_url": "https://cinestream-sulo-default-rtdb.firebaseio.com",
        "project_id": "cinestream-sulo",
        "storage_bucket": "cinestream-sulo.firebasestorage.app"
      },
      "client": [
        {
          "client_info": {
            "mobilesdk_app_id": "1:979447256418:android:037a9b3a1393e550a9883b",
            "android_client_info": {
              "package_name": "com.aistudio.cinestream.xyzabc"
            }
          },
          "oauth_client": [
            {
              "client_id": "979447256418-9jl4cl94f22a227strvhj3ups98dpka0.apps.googleusercontent.com",
              "client_type": 1,
              "android_info": {
                "package_name": "com.aistudio.cinestream.xyzabc",
                "certificate_hash": "1e0f4a4a0ee84b7023e59504b3a978382d019e2a"
              }
            },
            {
              "client_id": "979447256418-rjb9a0991gvve0328n113dme67i4gpv2.apps.googleusercontent.com",
              "client_type": 3
            }
          ],
          "api_key": [
            {
              "current_key": "AIzaSyC9CvaM9Mw3NiD-KsOiYvHZHj6XZJQJnPs"
            }
          ],
          "services": {
            "appinvite_service": {
              "other_platform_oauth_client": [
                {
                  "client_id": "979447256418-rjb9a0991gvve0328n113dme67i4gpv2.apps.googleusercontent.com",
                  "client_type": 3
                }
              ]
            }
          }
        },
        {
          "client_info": {
            "mobilesdk_app_id": "1:979447256418:android:ea5b570266ae1aa6a9883b",
            "android_client_info": {
              "package_name": "com.aistudio.cinestreamadmin.adcxz"
            }
          },
          "oauth_client": [
            {
              "client_id": "979447256418-m7u3batvou0mmrubfkmsnmunvpedo0dd.apps.googleusercontent.com",
              "client_type": 1,
              "android_info": {
                "package_name": "com.aistudio.cinestreamadmin.adcxz",
                "certificate_hash": "54a814fb04debd983f336ef82e87820edb760f12"
              }
            },
            {
              "client_id": "979447256418-rjb9a0991gvve0328n113dme67i4gpv2.apps.googleusercontent.com",
              "client_type": 3
            }
          ],
          "api_key": [
            {
              "current_key": "AIzaSyC9CvaM9Mw3NiD-KsOiYvHZHj6XZJQJnPs"
            }
          ],
          "services": {
            "appinvite_service": {
              "other_platform_oauth_client": [
                {
                  "client_id": "979447256418-rjb9a0991gvve0328n113dme67i4gpv2.apps.googleusercontent.com",
                  "client_type": 3
                }
              ]
            }
          }
        }
      ],
      "configuration_version": "1"
    }
    """.trimIndent()
  )
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.windowsizeclass)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.firestore)

  // Firebase Auth and Google Sign-In:
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
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
