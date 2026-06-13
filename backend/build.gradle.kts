import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.opencalc"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server Core
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-websockets:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-server-cors:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Security & Hashing (Fixes the mindrot/BCrypt errors)
    implementation("org.mindrot:jbcrypt:0.4")

    // Database - Exposed ORM + PostgreSQL
    implementation("org.jetbrains.exposed:exposed-core:0.52.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.52.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.52.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("com.zaxxer:HikariCP:5.1.0")



    // MinIO S3
    implementation("io.minio:minio:8.5.11")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:2.3.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "com.opencalc.backend.ApplicationKt"
    }
    archiveClassifier.set("all")
    mergeServiceFiles()
}

application {
    mainClass.set("com.opencalc.backend.ApplicationKt")
}