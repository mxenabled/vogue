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
@file:Suppress("TooManyFunctions")
package com.mx.vogue.core

import com.mx.vogue.core.exceptions.VogueProcessingException
import com.mx.vogue.core.models.DependencyContext
import com.mx.vogue.core.models.PackageRule
import com.mx.vogue.core.models.VersionsPluginDependency
import com.mx.vogue.core.models.VogueReport
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.streams.toList

const val MAX_FUTURE_DATE_MONTHS = 3L

fun isViolation(dependencyContext: DependencyContext): Boolean {
  return dependencyContext.maxMajorDiffExceeded ||
    dependencyContext.maxMinorDiffExceeded ||
    dependencyContext.maxMicroDiffExceeded ||
    dependencyContext.maxPatchVersionExceeded
}

fun getWarnings(report: VogueReport): List<DependencyContext> {
  return report.outdated.filter { !isViolation(it) }
}

fun getViolations(report: VogueReport): List<DependencyContext> {
  return report.outdated.filter { isViolation(it) }
}

fun getPackage(versionsPluginDependency: VersionsPluginDependency): String {
  return "${versionsPluginDependency.group}:${versionsPluginDependency.name}"
}

fun getPackageRule(versionsPluginDependency: VersionsPluginDependency, packageRules: List<PackageRule>): PackageRule? {
  return packageRules.firstOrNull {
    Regex(it.`package`).containsMatchIn(getPackage(versionsPluginDependency))
  }
}

@Suppress("MaxLineLength")
fun parseSuppressUntilDate(suppressUntil: String): LocalDate {
  return LocalDate.parse(suppressUntil.replace("/", "-"), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}

fun filterStaleSuppressions(packageRules: List<PackageRule>?): List<PackageRule> {
  if (packageRules == null) {
    return listOf()
  }

  return packageRules.stream().filter {
    if (it.suppressUntil == null) {
      return@filter true
    }
    parseSuppressUntilDate(it.suppressUntil!!).isAfter(LocalDate.now())
  }.toList()
}

@Suppress("MaxLineLength")
fun reportStaleSuppressions(packageRules: List<PackageRule>?) {
  if (packageRules == null) {
    return
  }

  val staleSuppressions = packageRules.stream().filter {
    if (it.suppressUntil == null) {
      return@filter false
    }
    parseSuppressUntilDate(it.suppressUntil!!).isBefore(LocalDate.now())
  }.toList()

  if (staleSuppressions.isNotEmpty()) {
    val stringBuilder = StringBuilder()

    stringBuilder.append(cyan("The following suppressions have gone stale. Please either remove them from the .vogue.yml or update the suppressUntil date.\n"))
    staleSuppressions.forEach {
      stringBuilder.append(" -> ${green(it.`package`)} expired on ${red(it.suppressUntil!!)} ${yellow("(${Duration.between(parseSuppressUntilDate(it.suppressUntil!!).atStartOfDay(), LocalDate.now().atStartOfDay()).toDays()} days ago)")}\n")
    }

    throw VogueProcessingException(stringBuilder.toString(), null)
  }
}

@Suppress("MaxLineLength", "ThrowsCount", "ReturnCount")
fun shouldSuppressPackageRule(packageRule: PackageRule?): Boolean {
  if (packageRule == null) {
    return false
  }

  if (packageRule.suppressUntil != null) {
    try {
      val suppressUntilDate = parseSuppressUntilDate(packageRule.suppressUntil!!)

      if (suppressUntilDate.isAfter(LocalDate.now().plusMonths(MAX_FUTURE_DATE_MONTHS))) {
        throw VogueProcessingException(red("Invalid 'suppressUntil' date provided: ${packageRule.suppressUntil}. The date must be within $MAX_FUTURE_DATE_MONTHS months of today."), null)
      }

      return suppressUntilDate.isAfter(LocalDate.now())
    } catch (e: DateTimeParseException) {
      throw VogueProcessingException(red("Invalid 'suppressUntil' date provided: ${packageRule.suppressUntil}. Please use one of the following formats: yyyy-MM-dd or yyyy/MM/dd."), e)
    }
  }

  return false
}
