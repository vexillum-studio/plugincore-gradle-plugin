package com.vexillum.gradle.plugincore

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.bundling.Jar

class PluginJarTask : Jar() {

    @Input
    var projectName: String? = null

    @Input
    var mainClass: String? = null

    init {
        assert(projectName != null)
        assert(mainClass != null)
        archiveFileName.set("$projectName.jar")

        manifest {
            attributes["Implementation-Title"] = projectName
            attributes["Main-Class"] = mainClass
            attributes["Implementation-Version"] = project.version.toString()
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        val runtimeClasspath = project.extensions
            .getByType(JavaPluginExtension::class.java)
            .sourceSets.getAt("main")
            .runtimeClasspath

        dependsOn(runtimeClasspath)

        from({
            runtimeClasspath
                .filter { it.name.endsWith("jar") }
                .map { project.zipTree(it) }
        })
    }

}
