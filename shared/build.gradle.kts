import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.serialization)
}

val generateJiminyBuildInfo = tasks.register("generateJiminyBuildInfo") {
    description = "generateJiminyBuildInfo"
    val outputDir = layout.buildDirectory.dir("generated/jiminy/src/commonMain/kotlin")
    val outputFile = outputDir.map { it.file("music/jiminy/JiminyBuildInfo.kt") }

    val gitHash = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText

    val projectVersion = provider { rootProject.version.toString() }

    inputs.property("gitHash", gitHash)
    inputs.property("version", projectVersion)
    outputs.dir(outputDir)

    doLast {
        val hash = gitHash.get().trim()
        val version = projectVersion.get()
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package music.jiminy

            object JiminyBuildInfo {
                const val GIT_HASH = "$hash"
                const val VERSION = "$version"
            }""".trimIndent()
        )
    }
}

kotlin {
    jvmToolchain(25)

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateJiminyBuildInfo)
            dependencies {
                // For the @Serializable annotation and Json.decode
                api(libs.kotlinx.serialization.json)
                api(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.collection.immutable)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
