/*
 * Copyright (c) MyScript. All rights reserved.
 */

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(Plugin.android)
        classpath(Plugin.kotlin)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts.kts.kts files
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.register("clean",Delete::class) {
	delete = setOf(rootProject.buildDir)
}
