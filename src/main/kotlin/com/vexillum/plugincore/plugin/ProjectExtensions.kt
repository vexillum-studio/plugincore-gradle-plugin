package com.vexillum.plugincore.plugin

import org.gradle.api.Project

fun Project.githubMavenRepo(
    repositoryName: String,
    organizationName: String
) {
    repositories.maven {
        url = uri("https://maven.pkg.github.com/$organizationName/$repositoryName")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_KEY")
        }
    }
}

fun Project.spigotRepository(releasesOnly: Boolean = false) {
    repositories.maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        mavenContent {
            if (releasesOnly) {
                releasesOnly()
            }
        }
    }
}

fun Project.vexillumRepo(
    repositoryName: String
) {
    githubMavenRepo(repositoryName, "vexillum-studio")
}
