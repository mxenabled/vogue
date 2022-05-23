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
package com.mx.vogue.core

import com.google.gson.Gson
import com.mx.vogue.core.models.Configuration
import com.mx.vogue.core.models.DependencyContext
import com.mx.vogue.core.models.Rule
import com.mx.vogue.core.models.Rules
import com.mx.vogue.core.models.VersionsPluginReport
import com.mx.vogue.core.models.VogueReport
import java.io.File
import java.io.FileNotFoundException

class Reporter constructor(private val config: Configuration) {

  fun generate(gradleVersionsReportPath: String): VogueReport {
    val gradleVersionsReport = loadGradleVersionsPluginReport(gradleVersionsReportPath)
    val vogueReport = VogueReport(upToDate = gradleVersionsReport.current)

    gradleVersionsReport.outdated.dependencies.forEach {
      val dependencyContext = DependencyContext(
        versionsPluginDependency = it,
        current = VersionNumber.parse(it.version),
        latest = VersionNumber.parse(it.available.release)
      )

      setAvailableUpgrades(dependencyContext)

      val rule = config.packageRules?.let { packageRules -> getPackageRule(it, packageRules) }

      if (shouldSuppressPackageRule(rule)) {
        rule?.suppressUntil?.let { suppressUntil ->
          dependencyContext.suppressUntil = suppressUntil
        }
      } else {
        val rules = rule?.rules ?: config.defaultRules ?: Rules()
        setRuleViolations(dependencyContext, rules)
      }

      vogueReport.outdated.add(dependencyContext)
    }

    return vogueReport
  }

  private fun setRuleViolations(dependencyContext: DependencyContext, rules: Rules) {
    if (dependencyContext.majorUpgradeAvailable && shouldApplyRule(rules.major)) {
      dependencyContext.maxMajorDiffExceeded = rules.major!!.maxDiff <
        (dependencyContext.latest.major - dependencyContext.current.major)
    } else if (dependencyContext.minorUpgradeAvailable && shouldApplyRule(rules.minor)) {
      dependencyContext.maxMinorDiffExceeded = rules.minor!!.maxDiff <
        (dependencyContext.latest.minor - dependencyContext.current.minor)
    } else if (dependencyContext.microUpgradeAvailable && shouldApplyRule(rules.micro)) {
      dependencyContext.maxMicroDiffExceeded = rules.micro!!.maxDiff <
        (dependencyContext.latest.micro - dependencyContext.current.micro)
    } else if (dependencyContext.patchUpgradeAvailable && shouldApplyRule(rules.patch)) {
      dependencyContext.maxPatchVersionExceeded = rules.patch!!.maxDiff <
        (dependencyContext.latest.patch - dependencyContext.current.patch)
    }
  }

  private fun setAvailableUpgrades(dependencyContext: DependencyContext) {
    if (dependencyContext.current.major != dependencyContext.latest.major) {
      dependencyContext.majorUpgradeAvailable = true
    } else if (dependencyContext.current.minor != dependencyContext.latest.minor) {
      dependencyContext.minorUpgradeAvailable = true
    } else if (dependencyContext.current.micro != dependencyContext.latest.micro) {
      dependencyContext.microUpgradeAvailable = true
    } else if (dependencyContext.current.patch != dependencyContext.latest.patch) {
      dependencyContext.patchUpgradeAvailable = true
    }
  }

  private fun loadGradleVersionsPluginReport(path: String): VersionsPluginReport {
    val file = File(path)
    if (!file.exists()) {
      throw FileNotFoundException("gradle-versions-plugin could not be loaded at $path")
    }

    return Gson().fromJson(file.readText(Charsets.UTF_8), VersionsPluginReport::class.java)
  }

  private fun shouldApplyRule(rule: Rule?): Boolean {
    return rule != null && (rule.maxDiff >= 0 || rule.requireLatest)
  }
}
