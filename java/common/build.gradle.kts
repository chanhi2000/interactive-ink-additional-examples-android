/*
 * Copyright (c) MyScript. All rights reserved.
 */

import com.myscript.gradle.tasks.CopyResourceAssetsTask

plugins {
    id("com.android.library")
}

android {
    compileSdkVersion(Versions.compileSdk)
    defaultConfig {
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    // myscript certificate.
    api(project(":certificate"))
    implementation(Dep.androidx)
    testImplementation(Dep.junit)
    androidTestImplementation(Dep.androidx_test)
    api(Dep.iink)    // iink SDK.
}

val copyResourceAssets by tasks.registering(CopyResourceAssetsTask::class)
tasks.matching { it.name == "preBuild" }.all {
    dependsOn(copyResourceAssets)
}

