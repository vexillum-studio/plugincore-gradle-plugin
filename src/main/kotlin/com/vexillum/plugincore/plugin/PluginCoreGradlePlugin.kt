@file:Suppress("unused")

package com.vexillum.plugincore.plugin

import java.io.File
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

class PluginCoreGradlePlugin : Plugin<Project> {

    abstract class PluginCoreExtension {
        var projectName: String? = null
        var author: String? = null
        var mainClass: String? = null
        var apiVersion: String? = null
        var pluginCoreVersion: String? = null
        var spigotVersion: String? = null
        var pluginYmlOutputDir: String? = null
        var githubURL: String? = null
        var publish: Boolean = true
    }

    private data class PluginData(
        val project: Project,
        val projectName: String,
        val author: String,
        val mainClass: String,
        val apiVersion: String,
        val pluginCoreVersion: String,
        var spigotVersion: String?,
        val pluginYmlOutputDir: String,
        val githubURL: String?,
        val gitHubDetails: GitHubDetails
    )

    private data class GitHubDetails(
        val username: String,
        val gprKey: String,
        val deployDetails: DeployDetails?
    )

    private data class DeployDetails(
        val organizationName: String,
        val repositoryName: String,
        val publish: Boolean
    )

    private fun PluginData.file(path: String): File =
        project.layout.projectDirectory.file(path).asFile

    private fun PluginCoreExtension.extractDetails(project: Project): GitHubDetails {
        val foundGitHubURL = this.githubURL
        val deployDetails = if (foundGitHubURL != null) {
            val regex = Regex("https://github\\.com/(?<org>[^/]+)/(?<repo>[^/]+)")
            val matchResult = regex.find(foundGitHubURL) ?: error("GitHub URL is invalid")
            DeployDetails(
                organizationName = matchResult.groups["org"]?.value!!,
                repositoryName = matchResult.groups["repo"]?.value!!,
                publish = publish
            )
        } else null

        return GitHubDetails(
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER") ?: error("GPR_USER not found in environment"),
            gprKey = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_KEY") ?: error("GPR_KEY not found in environment"),
            deployDetails = deployDetails
        )
    }

    override fun apply(project: Project) {
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.extensions.extraProperties["kotlin.version"] = "1.9.0"

        val extension = project.extensions.create(
            "pluginCore",
            PluginCoreExtension::class.java
        )

        project.afterEvaluate {
            println("PluginCore: Main class is set to ${extension.mainClass}")
            val data = with(extension) {
                val gitHubDetails = extractDetails(project)
                PluginData(
                    project = project,
                    projectName = projectName ?: project.name,
                    author = author ?: gitHubDetails.username,
                    mainClass = mainClass ?: error("`mainClass` pluginCore extension property is required"),
                    apiVersion = apiVersion ?: error("`apiVersion` pluginCore extension is required"),
                    pluginCoreVersion = pluginCoreVersion
                        ?: error("`pluginCoreVersion` pluginCore extension is required"),
                    spigotVersion = spigotVersion ?: error("`spigotVersion` pluginCore extension is required"),
                    pluginYmlOutputDir = pluginYmlOutputDir ?: "src/main/resources",
                    githubURL = githubURL,
                    gitHubDetails = gitHubDetails
                )
            }

            if ("${project.group}.${project.name}" != "com.vexillum.plugincore") {
                project.dependencies.add(
                    "compileOnly",
                    "com.vexillum:plugincore:${data.pluginCoreVersion}"
                )
            }
            project.dependencies.add(
                "compileOnly",
                "org.spigotmc:spigot-api:${data.spigotVersion}"
            )

            project.tasks.register("generatePluginYml").configure {
                generatePluginYml(data)
            }

            project.tasks.named("processResources") {
                dependsOn(project.tasks.named("generatePluginYml"))
            }

            project.tasks.register("pluginJar", Jar::class.java)
                .configure(configureJar(data))

            project.tasks.named("build") {
                dependsOn(project.tasks.named("pluginJar"))
            }

            project.tasks.register("printVersion", PrintVersionTask::class.java)

            project.tasks.register("generateCIWorkflows") {
                doLast {
                    generateCIWorkflows(data)
                }
            }

            applyPublication(data)
        }
    }

    private fun readResource(path: String): String {
        val resourceStream = PluginCoreGradlePlugin::class.java.classLoader.getResourceAsStream(path)
            ?: error("Missing resource at path: $path")
        return resourceStream.use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        }
    }

    private fun copyResourceTo(resourcePath: String, toFile: File) {
        toFile.mkdirs()
        toFile.writeText(readResource(resourcePath))
    }

    private fun generatePluginYml(data: PluginData) {
        val templateContent = readResource("templates/plugin.yml.template")
        val outputFile = data.file("${data.pluginYmlOutputDir}/plugin.yml")

        val content = with(data) {
            templateContent
                .replace("@PLUGIN_NAME@", projectName)
                .replace("@AUTHOR@", author)
                .replace("@PLUGIN_MAIN@", mainClass)
                .replace("@VERSION@", project.version.toString())
                .replace("@API_VERSION@", apiVersion)
        }

        outputFile.writeText(content)
        println("Generated plugin.yml at: ${outputFile.absolutePath}")
    }

    private fun configureJar(data: PluginData) =
        Action<Jar> {
            archiveFileName.set("${data.projectName}.jar")

            manifest {
                attributes["Implementation-Title"] = data.projectName
                attributes["Main-Class"] = data.mainClass
                attributes["Implementation-Version"] = project.version.toString()
            }

            duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            val runtimeClasspath = project.configurations.getByName("runtimeClasspath")

            dependsOn(runtimeClasspath)

            from({
                runtimeClasspath
                    .filter { it.name.endsWith(".jar") }
                    .map { project.zipTree(it) }
            })
        }

    private fun generateCIWorkflows(data: PluginData) {
        val deployDetails = data.gitHubDetails.deployDetails ?: error("CI workflows needs from githubURL to be defined")
        val githubWorkflows = data.file("templates/github/workflows")
        githubWorkflows.mkdirs()
        val deployContent = readResource("templates/github/workflows/deploy.yml")
            .replace("@ORG_NAME@", deployDetails.organizationName)
            .replace("@PACKAGE@", "${data.project.group}.${data.project.name.lowercase()}")
        val deployFile = githubWorkflows.resolve("deploy.yml")
        deployFile.writeText(deployContent)
        println("Generated workflow file deploy.yml at: ${deployFile.absolutePath}")
        val prFile = githubWorkflows.resolve("pr.yml")
        copyResourceTo("templates/github/workflows/pr.yml", prFile)
        println("Generated workflow file pr.yml at: ${deployFile.absolutePath}")
    }

    private fun applyPublication(data: PluginData) {
        val deployDetails = data.gitHubDetails.deployDetails
        if (deployDetails == null || !deployDetails.publish) {
            return
        }
        data.project.plugins.apply("maven-publish")
        data.project.extensions.configure(PublishingExtension::class.java) {
            repositories {
                data.project.githubMavenRepo(
                    repositoryName = deployDetails.repositoryName,
                    organizationName = deployDetails.organizationName
                )
            }
            publications {
                create("pluginRelease", MavenPublication::class.java) {
                    artifact(data.project.tasks.named("pluginJar"))
                }
            }
        }
    }

}
