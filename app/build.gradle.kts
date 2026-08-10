import org.gradle.api.tasks.testing.Test

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.battleheim.quantum2048"
    compileSdk = 35

    signingConfigs {
        create("release") {
            val storeFilePath = providers.gradleProperty("QUANTUM2048_RELEASE_STORE_FILE")
                .orElse(providers.environmentVariable("QUANTUM2048_RELEASE_STORE_FILE"))
                .orNull
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = providers.gradleProperty("QUANTUM2048_RELEASE_STORE_PASSWORD")
                    .orElse(providers.environmentVariable("QUANTUM2048_RELEASE_STORE_PASSWORD"))
                    .orNull
                keyAlias = providers.gradleProperty("QUANTUM2048_RELEASE_KEY_ALIAS")
                    .orElse(providers.environmentVariable("QUANTUM2048_RELEASE_KEY_ALIAS"))
                    .orNull
                keyPassword = providers.gradleProperty("QUANTUM2048_RELEASE_KEY_PASSWORD")
                    .orElse(providers.environmentVariable("QUANTUM2048_RELEASE_KEY_PASSWORD"))
                    .orNull
            }
        }
    }

    defaultConfig {
        applicationId = "com.battleheim.quantum2048"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0-quantum-core"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "512m"
    maxParallelForks = 1
    forkEvery = 25
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
