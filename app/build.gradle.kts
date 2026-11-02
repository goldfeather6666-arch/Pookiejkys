plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pixellab.suite"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pixellab.suite"
        minSdk = 29
        targetSdk = 35
        versionCode = 100
        versionName = "1.0.0-komodo"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Uses debug signing config fallback when CI keystore secrets are not injected
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Modular Clean Architecture directory boundaries mapped into the unified compilation graph
    sourceSets {
        getByName("main") {
            java.srcDirs(
                "src/main/kotlin",
                "../core/common/src/main/kotlin",
                "../core/designsystem/src/main/kotlin",
                "../core/database/src/main/kotlin",
                "../core/datastore/src/main/kotlin",
                "../core/permissions/src/main/kotlin",
                "../core/device/src/main/kotlin",
                "../core/diagnostics/src/main/kotlin",
                "../core/ui/src/main/kotlin",
                "../domain/src/main/kotlin",
                "../data/src/main/kotlin",
                "../feature/dashboard/src/main/kotlin",
                "../feature/device/src/main/kotlin",
                "../feature/automation/src/main/kotlin",
                "../feature/sensors/src/main/kotlin",
                "../feature/localai/src/main/kotlin",
                "../feature/network/src/main/kotlin",
                "../feature/battery/src/main/kotlin",
                "../feature/studio/src/main/kotlin",
                "../feature/productivity/src/main/kotlin",
                "../feature/privacy/src/main/kotlin",
                "../feature/settings/src/main/kotlin"
            )
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/kotlin")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
