package com.vexillum.gradle.plugincore

import java.io.File
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar

//import com.vexillum.gradle.plugincore.PluginYmlGenerator
//import com.vexillum.gradle.plugincore.PluginJarTask

class PluginCoreGradlePlugin : Plugin<Project> {

    internal data class PluginCoreData(
        val projectName: String,
        val author: String,
        val mainClass: String,
        val apiVersion: String,
        var mainYmlOutputDir: String? = null
    )

    override fun apply(project: Project) {
        val extension = project.extensions.create("pluginCore", PluginCoreExtension::class.java)
        val projectMainClass = extension.mainClass ?: error("`mainClass` pluginCore extension property is required")

        /*
        val srcDirs = listOf(
            project.layout.projectDirectory.file("src/main/kotlin/${projectMainClass.replace(".", "/") + ".kt"}"),
            project.layout.projectDirectory.file("src/main/java/${projectMainClass.replace(".", "/")}.java")
        )

        if (srcDirs.none { it.asFile.exists() }) {
            throw IllegalStateException("Main class '$projectMainClass' not found in the project.")
        }*/

        val data = PluginCoreData(
            projectName = (extension.projectName ?: project.name).lowercase(),
            author = extension.author ?: "Unknown",
            mainClass = projectMainClass,
            apiVersion = extension.apiVersion ?: "1.19",
            mainYmlOutputDir = extension.mainYmlOutputDir
        )

        val ymlTask = project.tasks.register("generatePluginYml").configure {
            val outputFile = project.layout.projectDirectory.file("src/main/resources/plugin.yml").asFile
            val templateFile = File(project.rootDir, "buildSrc/resources/plugin.yml.template")
            if (!templateFile.exists()) {
                error("Missing plugin.yml.template in buildSrc/resources")
            }

            val content = templateFile.readText()
                .replace("@PLUGIN_NAME@", data.projectName)
                .replace("@AUTHOR@", data.author    )
                .replace("@PLUGIN_MAIN@", data.mainClass)
                .replace("@VERSION@", project.version.toString())
                .replace("@API_VERSION@", data.apiVersion)

            outputFile.writeText(content)
            println("Generated plugin.yml at: ${outputFile.absolutePath}")
            /*pluginName = data.projectName
            author = data.author
            mainClass = data.mainClass
            version = project.version.toString()
            apiVersion = data.apiVersion
            data.mainYmlOutputDir?.let {
                outputDir = it
            }*/
        }

        project.tasks.named("processResources") {
            dependsOn(ymlTask)
        }

        val jarTask = project.tasks.register("pluginJar", Jar::class.java).configure {
            archiveFileName.set("${data.projectName}.jar")

            manifest {
                attributes["Implementation-Title"] = data.projectName
                attributes["Main-Class"] = data.mainClass
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
            /*projectName = data.projectName
            mainClass = data.mainClass*/
        }

        project.tasks.named("build") {
            dependsOn(jarTask)
        }

        project.tasks.register("printVersion") {
            doLast {
                println(project.version)
            }
        }
    }
}
