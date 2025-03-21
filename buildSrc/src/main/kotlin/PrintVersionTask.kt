package com.vexillum.plugincore.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class PrintVersionTask : DefaultTask() {
    init {
        group = "Versioning"
        description = "Prints the project version"
    }

    @TaskAction
    fun printVersion() {
        println(project.version)
    }
}