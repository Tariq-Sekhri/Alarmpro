plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

val localBuildRoot = file("${System.getenv("LOCALAPPDATA")}/Alarmpro-build")

layout.buildDirectory.set(localBuildRoot.resolve("root"))

subprojects {
    layout.buildDirectory.set(localBuildRoot.resolve(project.name))
}