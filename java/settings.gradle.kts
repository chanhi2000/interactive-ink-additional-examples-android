/*
 * Copyright (c) MyScript. All rights reserved.
 */

rootProject.name = "myscript.iink.samples.android.java"

val myProjects = mapOf(
    // samples
    ":batch-mode-sample"            to file("$settingsDir/samples/batch-mode"),
    ":exercise-assessment-sample"   to file("$settingsDir/samples/exercise-assessment"),
    ":search-sample"                to file("$settingsDir/samples/search"),
    ":lasso-sample"                 to file("$settingsDir/samples/lasso"),
    // MyScript certificate
    ":certificate"                  to file("${settingsDir.parent}/certificate"),
    // common libraries
    ":common"                       to file("$settingsDir/common"),
    // MyScript iink UI reference implementation.
    // Copy from: https://github.com/MyScript/interactive-ink-examples-android/tree/43170e5ab239c15818dc254c4a68cdbc96f6ba27/UIReferenceImplementation
    ":UIReferenceImplementation"    to file("${settingsDir.parent}/UIReferenceImplementation")
)

myProjects.forEach { (myProject, dir) ->
    logger.info("Including $myProject")
    include(myProject)
    project(myProject).projectDir = dir
}
