rootProject.name = "plugin"

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()

        maven {
            url = uri("https://maven.pkg.github.com/vexillum-studio/plugincore")
            credentials {
                username = System.getenv("GPR_USER")
                password = System.getenv("GPR_KEY")
            }
        }
    }
}

