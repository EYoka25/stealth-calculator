plugins {
    id("com.android.application") version "7.4.2"
    id("org.jetbrains.kotlin.android") version "1.9.20"
    id("kotlin-kapt")
}

android {
    namespace = "com.example.stealthcalculator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.stealthcalculator"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-websockets:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("androidx.lifecycle:lifecycle-process:2.8.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
