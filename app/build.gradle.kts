import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Load keystore from local.properties if exists (for local dev)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.auraai.overlay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.auraai.overlay"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("BUILD_NUMBER") ?: "1").toInt()
        versionName = System.getenv("RELEASE_VERSION") ?: "1.0.0"
    }

    signingConfigs {
        create("release") {
            val ksFile = System.getenv("KEYSTORE_PATH")?.let { file(it) }
                ?: keystorePropertiesFile.takeIf { it.exists() }?.let {
                    file(keystoreProperties["storeFile"] as? String ?: "")
                }
            if (ksFile != null && ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: keystoreProperties["storePassword"] as? String ?: ""
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: keystoreProperties["keyAlias"] as? String ?: ""
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: keystoreProperties["keyPassword"] as? String ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.markwon.core)
}
