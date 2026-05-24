import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val cameraXVersion = "1.4.1"
val secureProperties = Properties().apply {
    val secureFile = rootProject.file("secure.properties")
    if (secureFile.exists()) {
        secureFile.inputStream().use(::load)
    }
}

fun requiredSecureConfig(key: String): String =
    (secureProperties.getProperty(key)
        ?: providers.gradleProperty(key).orNull
        ?: providers.environmentVariable(key).orNull)
        ?.takeIf { it.isNotBlank() }
        ?: error(
            "Missing Android secure config '$key'. " +
                "Define it in apps/android/secure.properties, as a Gradle property, or as an environment variable.",
        )

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
        buildConfigField("String", "AWS_REGION", "\"${requiredSecureConfig("CT_AWS_REGION")}\"")
        buildConfigField("String", "COGNITO_DOMAIN", "\"${requiredSecureConfig("CT_COGNITO_DOMAIN")}\"")
        buildConfigField("String", "COGNITO_USER_POOL_ID", "\"${requiredSecureConfig("CT_COGNITO_USER_POOL_ID")}\"")
        buildConfigField("String", "COGNITO_ANDROID_CLIENT_ID", "\"${requiredSecureConfig("CT_COGNITO_ANDROID_CLIENT_ID")}\"")
        buildConfigField("String", "SYNC_API_BASE_URL", "\"${requiredSecureConfig("CT_SYNC_API_BASE_URL")}\"")
        buildConfigField("String", "COGNITO_ANDROID_REDIRECT_URI", "\"${requiredSecureConfig("CT_COGNITO_ANDROID_REDIRECT_URI")}\"")
        buildConfigField("String", "COGNITO_ANDROID_LOGOUT_URI", "\"${requiredSecureConfig("CT_COGNITO_ANDROID_LOGOUT_URI")}\"")
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
    implementation("androidx.compose.foundation:foundation:1.11.2")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha12")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.google.guava:guava:33.4.8-android")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-ktx:2.7.0")
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    ksp("androidx.room:room-compiler:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
