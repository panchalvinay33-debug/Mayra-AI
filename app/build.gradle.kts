plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

fun env(name: String): String? = System.getenv(name)?.takeIf(String::isNotBlank)

val mayraReleaseStoreFile = env("MAYRA_RELEASE_STORE_FILE")
val mayraReleaseStorePassword = env("MAYRA_RELEASE_STORE_PASSWORD")
val mayraReleaseKeyAlias = env("MAYRA_RELEASE_KEY_ALIAS")
val mayraReleaseKeyPassword = env("MAYRA_RELEASE_KEY_PASSWORD")
val mayraReleaseSigningAvailable = listOf(
    mayraReleaseStoreFile,
    mayraReleaseStorePassword,
    mayraReleaseKeyAlias,
    mayraReleaseKeyPassword
).all { it != null }

val mayraOwnerStoreFile = env("MAYRA_OWNER_STORE_FILE")
val mayraOwnerStorePassword = env("MAYRA_OWNER_STORE_PASSWORD")
val mayraOwnerKeyAlias = env("MAYRA_OWNER_KEY_ALIAS")
val mayraOwnerKeyPassword = env("MAYRA_OWNER_KEY_PASSWORD")
val mayraOwnerSigningAvailable = listOf(
    mayraOwnerStoreFile,
    mayraOwnerStorePassword,
    mayraOwnerKeyAlias,
    mayraOwnerKeyPassword
).all { it != null }

android {
    namespace = "ai.mayra.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.mayra.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (mayraReleaseSigningAvailable) {
            create("mayraRelease") {
                storeFile = file(mayraReleaseStoreFile!!)
                storePassword = mayraReleaseStorePassword
                keyAlias = mayraReleaseKeyAlias
                keyPassword = mayraReleaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
        if (mayraOwnerSigningAvailable) {
            create("mayraOwner") {
                storeFile = file(mayraOwnerStoreFile!!)
                storePassword = mayraOwnerStorePassword
                keyAlias = mayraOwnerKeyAlias
                keyPassword = mayraOwnerKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("mayraRelease")?.let { signingConfig = it }
            buildConfigField("boolean", "STABLE_OWNER_SIGNING", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("personalAlpha") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".alpha"
            versionNameSuffix = "-alpha"
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("mayraOwner") ?: signingConfigs.getByName("debug")
            buildConfigField("boolean", "STABLE_OWNER_SIGNING", mayraOwnerSigningAvailable.toString())
            matchingFallbacks += listOf("debug")
        }
        create("fullTest") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".fulltest"
            versionNameSuffix = "-fulltest"
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "STABLE_OWNER_SIGNING", "false")
            matchingFallbacks += listOf("debug")
        }
        create("documentTest") {
            initWith(getByName("release"))
            applicationIdSuffix = ".documenttest"
            isDebuggable = false
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "STABLE_OWNER_SIGNING", "false")
            matchingFallbacks += listOf("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.room:room-testing:2.7.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.14.1")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
