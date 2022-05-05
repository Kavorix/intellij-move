package org.move.openapiext

import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import org.move.cli.runconfig.AptosCommandConfiguration

val Project.runManager: RunManager get() = RunManager.getInstance(this)

fun Project.aptosRunConfigurations(): List<AptosCommandConfiguration> =
    runManager.allConfigurationsList
        .filterIsInstance<AptosCommandConfiguration>()

fun Project.aptosBuildRunConfigurations(): List<AptosCommandConfiguration> =
    aptosRunConfigurations().filter { it.command.startsWith("move compile") }
