/*
 * Copyright (c) MyScript. All rights reserved.
 */


plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdkVersion(Versions.compileSdk)
    defaultConfig {
        applicationId = "com.myscript.iink.samples.batchmode"
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode = Versions.iinkVersionCode
        versionName = Versions.iinkVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "${JavaVersion.VERSION_11}"
        useIR = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":common"))
    implementation(project(":UIReferenceImplementation"))

    implementation(Dep.androidx)
    implementation(Dep.gson)

    testImplementation(Dep.junit)
    androidTestImplementation(Dep.androidx_test)
}