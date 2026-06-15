// Top-level build file where you can add configuration options common to all sub-projects/modules.

// The Meta Spatial SDK plugin drags kotlin-compiler-embeddable onto the shared buildscript
// classpath, which collides with the Kotlin Gradle plugin's Build Tools API under AGP 9.
// Exclude it so classpath snapshot transforms use a single, consistent CompilationService.
buildscript {
    configurations.classpath {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
    alias(libs.plugins.meta.spatial.plugin) apply false
}