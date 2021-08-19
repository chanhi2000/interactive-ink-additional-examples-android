/*
 * Copyright (c) MyScript. All rights reserved.
 */
import org.gradle.api.artifacts.dsl.DependencyHandler

object Dep {
	val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
	val kotlin_stdlib_jre8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
	val androidx = listOf(
		"androidx.appcompat:appcompat:${Versions.androidx_appcompat}",
		"androidx.constraintlayout:constraintlayout:${Versions.androidx_constraintlayout}",
	)
	val androidx_test = listOf(
		"androidx.test:runner:${Versions.androidx_testRunner}",
    	"androidx.test.espresso:espresso-core:${Versions.androidx_espresso}",
	)
	val junit = "junit:junit:${Versions.junit}"

	val gson = "com.google.code.gson:gson:${Versions.gson}"

	val iink = "com.myscript:iink:${Versions.iinkVersionName}"
}

//util functions for adding the different type dependencies from build.gradle file
fun DependencyHandler.kapt(list: List<String>) {
	list.forEach { dependency ->
		add("kapt", dependency)
	}
}

fun DependencyHandler.implementation(list: List<String>) {
	list.forEach { dependency ->
		add("implementation", dependency)
	}
}

fun DependencyHandler.androidTestImplementation(list: List<String>) {
	list.forEach { dependency ->
		add("androidTestImplementation", dependency)
	}
}

fun DependencyHandler.testImplementation(list: List<String>) {
	list.forEach { dependency ->
		add("testImplementation", dependency)
	}
}