// ============================================================================
// ADD THESE TO YOUR EXISTING app/build.gradle.kts FILE
// ============================================================================

// 1. In the plugins block (top of file), ADD:
// alias(libs.plugins.kapt)

// 2. In the android block, ensure viewBinding is enabled (should already be there):
// buildFeatures {
//     viewBinding = true
//     buildConfig = true
// }

// 3. ADD these to the dependencies block:
dependencies {
    // Existing dependencies remain unchanged...
    // ADD the following new dependencies:

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Ktor Client (WebSocket + HTTP)
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-websockets:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // RecyclerView (should already be present via dependencies, but ensure it's there)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle Process (for ON_STOP/ON_PAUSE detection)
    implementation("androidx.lifecycle:lifecycle-process:2.8.3")
}

// ============================================================================
// ALSO ADD to the top of the file (if not present):
// import org.jetbrains.kotlin.kapt3.base.Kapt.kapt
// ============================================================================
