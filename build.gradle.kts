plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

gradlePlugin {
    plugins.create("forkyPlugin").apply {
        id = "io.github.forky"
        implementationClass = "io.github.forky.ForkyPlugin"
    }
}

group = "io.github.forky"
version = "0.1.0"

dependencies {
    implementation(libs.kotlin.utils)
    implementation(libs.gradle.utils)
    implementation(libs.kotlin.regex)
    implementation(libs.kotlin.shell)

    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.toml)
    implementation(libs.kotlin.eval)

    implementation(libs.tika)

    compileOnly(libs.android.tools)

    implementation(gradleApi())
}
