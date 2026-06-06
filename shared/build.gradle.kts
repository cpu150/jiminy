import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.serialization)
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
        commonMain.dependencies {
            // For the @Serializable annotation and Json.decode
            api(libs.kotlinx.serialization.json)
            api(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.collection.immutable)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
