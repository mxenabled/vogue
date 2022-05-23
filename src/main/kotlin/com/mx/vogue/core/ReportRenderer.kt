package com.mx.vogue.core

import com.mx.vogue.core.models.DependencyContext
import com.mx.vogue.core.models.VogueReport

fun renderReport(report: VogueReport): String {
  return buildString {
    if (report.upToDate.count > 0) {
      append(cyan("Up To Date:\n"))
      report.upToDate.dependencies.forEach {
        append(" - ${green(getPackage(it))} [${green(it.version)}]\n")
      }
      append("\n")
    }

    val warnings = getWarnings(report)
    val violations = getViolations(report)

    if (warnings.isNotEmpty()) {
      append(cyan("Warnings (should be upgraded ASAP):\n"))
      append(buildUpgradeMessages(warnings, ::buildWarningUpgradeMessage))
      append("\n")
    }

    if (violations.isNotEmpty()) {
      append(cyan("Errors (must be upgraded)\n"))
      append(buildUpgradeMessages(violations, ::buildErrorUpgradeMessage))
    }
  }
}

private fun buildUpgradeMessages(dependencyContexts: List<DependencyContext>, buildUpgradeMessage: (DependencyContext) -> String): String {
  return buildString {
    val majorUpgrades = dependencyContexts.filter { it.majorUpgradeAvailable }
    val minorUpgrades = dependencyContexts.filter { it.minorUpgradeAvailable }
    val microUpgrades = dependencyContexts.filter { it.microUpgradeAvailable }
    val patchUpgrades = dependencyContexts.filter { it.patchUpgradeAvailable }

    if (majorUpgrades.isNotEmpty()) {
      append("${green("MAJOR")} version upgrades available:\n")
      majorUpgrades.forEach { append(buildUpgradeMessage(it)) }
      append("\n")
    }

    if (minorUpgrades.isNotEmpty()) {
      append("${green("MINOR")} version upgrades available:\n")
      minorUpgrades.forEach { append(buildUpgradeMessage(it)) }
      append("\n")
    }

    if (microUpgrades.isNotEmpty()) {
      append("${green("PATCH")} version upgrades available:\n")
      microUpgrades.forEach { append(buildUpgradeMessage(it)) }
      append("\n")
    }

    if (patchUpgrades.isNotEmpty()) {
      append("${green("MICRO")} version upgrades available:\n")
      patchUpgrades.forEach { append(buildUpgradeMessage(it)) }
      append("\n")
    }
  }
}

@Suppress("MaxLineLength")
private fun buildWarningUpgradeMessage(dependencyContext: DependencyContext): String {
  return " - ${yellow(getPackage(dependencyContext.versionsPluginDependency))} [${green(dependencyContext.current.toString())} -> ${green(dependencyContext.latest.toString())}]\n"
}

@Suppress("MaxLineLength")
private fun buildErrorUpgradeMessage(dependencyContext: DependencyContext): String {
  return " - ${red(getPackage(dependencyContext.versionsPluginDependency))} [${green(dependencyContext.current.toString())} -> ${green(dependencyContext.latest.toString())}]\n"
}
