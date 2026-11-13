import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

ksp {
    // Keep a copy of each database version so future updates can migrate children's progress safely.
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.samjhoenglish"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.samjhoenglish"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Override for local testing: gradlew assembleDebug -PschoolApiUrl=http://localhost:8788
        val schoolApiUrl = providers.gradleProperty("schoolApiUrl").getOrElse("https://samjho-school.pages.dev")
        buildConfigField("String", "SCHOOL_API_URL", "\"$schoolApiUrl\"")
        manifestPlaceholders["usesCleartext"] = schoolApiUrl.startsWith("http://").toString()

        // The read-along listener (sherpa-onnx) ships native code; phones only need these two.
        ndk { abiFilters += setOf("arm64-v8a", "armeabi-v7a") }
    }

    // The real signing key lives outside git (keystore.properties and its .jks). Every app update
    // must be signed with this same key, or phones will refuse it.
    val keystoreFile = rootProject.file("keystore.properties")
    val releaseSigning = if (keystoreFile.exists()) {
        val keys = Properties().apply { keystoreFile.inputStream().use { load(it) } }
        signingConfigs.create("release") {
            storeFile = rootProject.file(keys.getProperty("storeFile"))
            storePassword = keys.getProperty("storePassword")
            keyAlias = keys.getProperty("keyAlias")
            keyPassword = keys.getProperty("keyPassword")
        }
    } else {
        null
    }

    buildTypes {
        release {
            // Off for now: shrinking stalls on this laptop's 8 GB, and it would need testing against JSON and Room.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning
            // Lets a release build be tried beside the installed app without replacing it.
            providers.gradleProperty("releaseTestSuffix").orNull?.let { applicationIdSuffix = it }
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // Checks for new school tests about once an hour, even when the app is closed.
    implementation(libs.androidx.work.runtime)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Read-along test only (offline Indian English listener). Model: python tools/get_vosk_model.py
    debugImplementation("com.alphacephei:vosk-android:0.3.75")
    debugImplementation("net.java.dev.jna:jna:5.18.1@aar")
    testImplementation(libs.junit)
}
