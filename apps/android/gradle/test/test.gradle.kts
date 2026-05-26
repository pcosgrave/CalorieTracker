plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    namespace = "com.philipcosgrave.calorietracker.test"
    compileSdk = 35

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Redirect System.out for better test output
                it.systemProperties["java.io.tmpdir"] = files("${projectDir}/tmp").asFile.absolutePath
            }
        }
    }

    buildTypes {
        getByName("test") {
            isDebuggable = true
        }
    }
}

dependencies {
    // JUnit
    implementation("junit:junit:4.13.2")
    implementation("org.mockito:mockito-core:5.15.2")
    implementation("org.mockito:mockito-android:5.15.2")
    implementation("org.mockito:mockito-inline:5.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    implementation("androidx.arch.core:core-testing:2.2.0")

    // Espresso for UI testing
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.8")
    androidTestImplementation("androidx.compose.ui:ui-testManifest:1.7.8")

    // Room testing
    debugImplementation("androidx.room:room-testing:2.6.1")

    // Coroutines testing
    debugImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // Truth for assertions
    implementation("org.truth0:truth:1.1.3")

    // Json testing
    implementation("org.json:json:20240303")
}
