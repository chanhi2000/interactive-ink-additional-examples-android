plugins {
    id("com.android.library")
}

android {
    compileSdkVersion(Versions.compileSdk)
    defaultConfig {
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        vectorDrawables.useSupportLibrary = true
    }
}

dependencies {
    implementation(Dep.androidx)
    implementation(Dep.gson)
    api(Dep.iink)
}
