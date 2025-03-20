import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.vexillum.plugincore"
version = "0.0.3"

repositories {
    mavenLocal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("plugin") {
            id = "com.vexillum.plugincore.plugin"
            implementationClass = "com.vexillum.plugincore.plugin.PluginCoreGradlePlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/vexillum-studio/plugincore-gradle-plugin")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_KEY")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.vexillum.plugincore"
            artifactId = "plugin"
            version = version

            from(components["java"])
        }
    }
}

project.tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
