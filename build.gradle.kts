plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

val localAppData = System.getenv("LOCALAPPDATA")
if (!localAppData.isNullOrBlank()) {
    // Keep generated files outside OneDrive. The previous build tree can remain
    // locked by Android Studio/indexing, so use a fresh stable local directory.
    val localBuildRoot = file("$localAppData/Alarmpro-build-active")
    layout.buildDirectory.set(localBuildRoot.resolve("root"))
    subprojects {
        layout.buildDirectory.set(localBuildRoot.resolve(project.name))
    }
}
