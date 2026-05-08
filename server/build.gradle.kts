plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "music.jiminy"
version = "1.0.0"

application {
    mainClass.set("music.jiminy.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.ktor.serverLogging)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serverWebsockets)
    // Specifically for Ktor WebSockets serialization
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

kotlin {
    jvmToolchain(25)
}

val copyWasmResources = tasks.register<Copy>("copyWasmResources") {
    val wasmTask = project(":composeApp").tasks.named("wasmJsBrowserDistribution")
    from(wasmTask.map { it.outputs.files })
    into(layout.buildDirectory.dir("resources/main/static"))
}

// This satisfies the error by creating an explicit dependency
tasks.named("shadowJar") {
    dependsOn(copyWasmResources)
}

// If your Ktor wizard used the Ktor plugin specifically:
tasks.maybeCreate("buildFatJar").apply {
    dependsOn(copyWasmResources)
}
