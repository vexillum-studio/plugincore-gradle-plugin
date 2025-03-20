# plugincore-gradle-plugin
This utility plugin adds automatic plugincore dependencies,
adds deploy and CI utilities, plugin.yml generation and other handy utilities
for minecraft plugin development with plugincore.

### How to use it
In your repositories add the packages distribution of this plugin, you may need to define the credentials with GPR keys
on the environment.
````kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/vexillum-studio/plugincore-gradle-plugin")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_KEY")
        }
    }
}
````
In your plugins block add this:
````kotlin
plugins {
    id("com.vexillum.plugincore.plugin") version "0.0.2" // Check for the latest version released
}

````
At last, add this extension to your `build.gradle.kts` and customize your config

````kotlin
pluginCore {
    projectName = "ExamplePlugin"
    author = "YourUserName"
    mainClass = "com.example.Main"
    apiVersion = "1.19"
    pluginCoreVersion = "0.1.5"
    spigotVersion = "1.21.4-R0.1-SNAPSHOT"
    // Used to make automatic publication and CI workflows
    githubURL = "https://github.com/vexillum-studio/plugin-template"
}

````
Check for the custom gradle tasks added by this plugin! 
