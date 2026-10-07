import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.play.publisher)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

val releaseStoreFile = keystoreProperties.getProperty("storeFile")
    ?: System.getenv("ANDROID_KEYSTORE_PATH")
val releaseStorePassword = keystoreProperties.getProperty("storePassword")
    ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = keystoreProperties.getProperty("keyAlias")
    ?: System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = keystoreProperties.getProperty("keyPassword")
    ?: System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

val ciVersionCode = providers.gradleProperty("ciVersionCode").map(String::toInt)
val ciVersionName = providers.gradleProperty("ciVersionName")

android {
    namespace = "ca.sekhrit.alarmpro"
    compileSdk = 36

    defaultConfig {
        applicationId = "ca.sekhrit.alarmpro"
        minSdk = 26
        targetSdk = 36
        versionCode = ciVersionCode.orNull ?: 39
        versionName = ciVersionName.orNull ?: "0.8.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        signingConfigs {
            create("release") {
                if (hasReleaseSigning) {
                    storeFile = file(requireNotNull(releaseStoreFile))
                    storePassword = releaseStorePassword
                    keyAlias = releaseKeyAlias
                    keyPassword = releaseKeyPassword
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "FULL"
            }
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

play {
    useApplicationDefaultCredentials = true
    track.set("internal")
    defaultToAppBundles.set(true)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material)
    implementation(libs.reorderable)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
