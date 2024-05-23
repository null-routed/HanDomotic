// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.0-RC3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0-RC3" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.0.0" apply false
    alias(libs.plugins.android.library) apply false
}

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}
