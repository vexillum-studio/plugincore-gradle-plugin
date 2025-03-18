package com.vexillum.gradle.plugincore

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

fun JavaExec.environmentIfMissing(name: String, value: Any) {
    if (name !in environment) {
        environment(name, value)
    }
}

fun Test.environmentIfMissing(name: String, value: Any) {
    if (name !in environment) {
        environment(name, value)
    }
}