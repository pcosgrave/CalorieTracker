import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val cameraXVersion = "1.4.1"

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.philipcosgrave.calorietracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.philipcosgrave.calorietracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "AWS_REGION", "\"us-east-1\"")
        buildConfigField("String", "COGNITO_DOMAIN", "\"philip-calorie-tracker-dev\"")
        buildConfigField("String", "COGNITO_USER_POOL_ID", "\"us-east-1_WYQwdC4oO\"")
        buildConfigField("String", "COGNITO_ANDROID_CLIENT_ID", "\"bb27drot68rek496i4tmrn2ik\"")
        buildConfigField("String", "SYNC_API_BASE_URL", "\"https://84jfkxkrd6.execute-api.us-east-1.amazonaws.com/dev\"")
        buildConfigField("String", "COGNITO_ANDROID_REDIRECT_URI", "\"calorietracker://auth/callback\"")
        buildConfigField("String", "COGNITO_ANDROID_LOGOUT_URI", "\"calorietracker://signout\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

configurations.configureEach {
    resolutionStrategy {
        force(
            "androidx.camera:camera-camera2:$cameraXVersion",
            "androidx.camera:camera-core:$cameraXVersion",
            "androidx.camera:camera-lifecycle:$cameraXVersion",
            "androidx.camera:camera-view:$cameraXVersion",
            "androidx.camera:camera-video:$cameraXVersion",
            "androidx.camera:camera-camera2-pipe:$cameraXVersion",
        )
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-ktx:2.7.0")
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    ksp("androidx.room:room-compiler:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
