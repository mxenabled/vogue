/**
 * Copyright 2020 MX Technologies.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mx.vogue

import com.github.benmanes.gradle.versions.VersionsPlugin
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.mx.vogue.core.Reporter
import com.mx.vogue.core.Suppressor
import com.mx.vogue.core.cyan
import com.mx.vogue.core.exceptions.VogueRuleViolation
import com.mx.vogue.core.filterStaleSuppressions
import com.mx.vogue.core.getViolations
import com.mx.vogue.core.green
import com.mx.vogue.core.loadConfiguration
import com.mx.vogue.core.loadDefaultConfiguration
import com.mx.vogue.core.mergeConfiguration
import com.mx.vogue.core.models.Configuration
import com.mx.vogue.core.red
import com.mx.vogue.core.renderReport
import com.mx.vogue.core.reportStaleSuppressions
import com.mx.vogue.core.writeConfiguration
import com.mx.vogue.core.yellow
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

class VoguePlugin : Plugin<Project> {
  @Suppress("ktlint:standard:max-line-length")
  override fun apply(project: Project) {
    var dependenciesExtension = project.extensions.create("vogue", VogueDependenciesExtension::class.java)

    project.afterEvaluate {
      // **************************************
      // Dependency Update Report
      // Configuration options:
      //   https://github.com/ben-manes/gradle-versions-plugin#dependencyupdates
      // **************************************
      project.plugins.apply(VersionsPlugin::class.java)
      project.tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
        it.outputFormatter = "json"
        it.outputDir = dependenciesExtension.dependencyUpdatesOutputDir
        it.reportfileName = "report"
        it.revision = "release"
        it.logging.captureStandardOutput(LogLevel.DEBUG)

        if (dependenciesExtension.excludePreReleaseVersions) {
          it.rejectVersionIf { selection -> selection.candidate.version.lowercase().endsWith("pre") }
          it.rejectVersionIf { selection -> selection.candidate.version.lowercase().contains("-rc.") }
        }
      }

      // If this is configured with Coppuccino, let's add our check to the list.
      if (project.tasks.findByName("check") != null) {
        project.tasks.getByName("check").finalizedBy("vogueReport")
      }

      project.tasks.named("vogueReport")
        .get()
        .dependsOn(project.tasks.named("dependencyUpdates"))

      project.tasks.named("vogueSuppress")
        .get()
        .dependsOn(project.tasks.named("dependencyUpdates"))
    }

    project.tasks.register("vogueReport") { task ->
      task.doLast {
        val configuration = loadEffectiveConfiguration()

        reportStaleSuppressions(configuration.packageRules)

        val report = Reporter(configuration).generate("${dependenciesExtension.dependencyUpdatesOutputDir}/report.json")

        println(renderReport(report))

        val violations = getViolations(report).size
        if (violations > 0) {
          throw VogueRuleViolation("${red("There are")} ${yellow("$violations")} ${red("rule violations that must be fixed.")} ${cyan("Please refer to the report output above.")}")
        }
      }
    }

    project.tasks.register("vogueSuppress") { task ->
      task.doLast {
        val configuration = loadEffectiveConfiguration()
        var overrides = loadConfiguration(".vogue.yml")

        if (overrides != null) {
          println(green("Cleaning up stale suppressions ...\n"))
          overrides.packageRules = filterStaleSuppressions(overrides.packageRules).toMutableList()
          configuration.packageRules = filterStaleSuppressions(configuration.packageRules).toMutableList()
          println(green("Done\n"))
        } else {
          overrides = Configuration()
        }

        val report = Reporter(configuration).generate("${dependenciesExtension.dependencyUpdatesOutputDir}/report.json")
        Suppressor().suppressViaUserInput(overrides, getViolations(report))

        println(green("Updating ${yellow(".vogue.yml")} ...\n"))
        overrides.packageRules = overrides.packageRules?.sortedBy { d -> d.`package` }?.toMutableList()
        writeConfiguration(".vogue.yml", overrides)
        println(green("Done\n"))
      }
    }
  }

  private fun loadEffectiveConfiguration(): Configuration {
    val defaultConfig = loadDefaultConfiguration()
    val overrides = loadConfiguration(".vogue.yml")
    mergeConfiguration(defaultConfig, overrides)
    return defaultConfig
  }
}
