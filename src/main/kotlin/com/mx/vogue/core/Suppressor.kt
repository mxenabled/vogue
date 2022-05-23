package com.mx.vogue.core

import com.mx.vogue.core.models.Configuration
import com.mx.vogue.core.models.DependencyContext
import com.mx.vogue.core.models.PackageRule
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class Suppressor {
  companion object {
    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    const val DEFAULT_SUPPRESSION_DAYS: Long = 5
    const val MAX_SUPPRESSION_MONTHS: Long = 3
  }

  // Walks the user through their dependency violations and helps them suppress them if they want.
  fun suppressViaUserInput(config: Configuration, violations: List<DependencyContext>) {
    if (config.packageRules == null) {
      config.packageRules = mutableListOf()
    }
    violations.forEach {
      // Ask the user whether they would like to suppress the current artifact.
      val artifact = "${it.versionsPluginDependency.group}:${it.versionsPluginDependency.name}"
      println("${green("Suppress")} ${yellow(artifact)}? [Y/n]")

      if (confirmed(readLine()!!.lowercase())) {
        handleUserSuppression(config, artifact)
      } else {
        println(cyan("Skipping.\n"))
      }
    }
  }

  private fun appendSuppression(config: Configuration, artifact: String, suppressUntil: String) {
    println("${cyan("Suppressed")} ${yellow(artifact)} until ${cyan(suppressUntil)}\n")
    val packageRule = PackageRule()
    packageRule.`package` = artifact
    packageRule.suppressUntil = suppressUntil
    config.packageRules!!.add(packageRule)
  }
  private fun confirmed(input: String): Boolean {
    return input.isBlank() || input == "y"
  }

  @Suppress("LoopWithTooManyJumpStatements")
  fun handleUserSuppression(config: Configuration, artifact: String) {
    val packageRule = PackageRule()
    packageRule.`package` = artifact

    // If the user has chosen to suppress the current artifact, ask them how long it should be suppressed for.
    // If they enter a bad date, keep re-prompting them until they either skip it
    val defaultSuppressionDate = LocalDate.now().plusDays(DEFAULT_SUPPRESSION_DAYS).format(DATE_FORMAT)
    do {
      println("\n${green("Suppress until")}? [${yellow("Default: $defaultSuppressionDate")}]")
      val suppressUntil = readLine()!!.lowercase()
      if (confirmed(suppressUntil)) {
        // The user is happy with the default suppression we suggested. Let's use it.
        appendSuppression(config, artifact, defaultSuppressionDate)
        break
      } else if (rejected(suppressUntil)) {
        // The user changed their mind and would rather skip suppressing this artifact. Let's abort.
        println(cyan("Skipping."))
        break
      } else if (isValidDate(suppressUntil)) {
        // The user has provided their own suppressUntil date, and it passed our input validation. Let's use it.
        appendSuppression(config, artifact, suppressUntil)
        break
      } else {
        // The user didn't accept, reject, or provide a valid suppressUntil date. Let's re-prompt them.
        println(
          "${red("Invalid date")}. " +
            "${yellow("Please input a date")} " +
            "(${cyan("within 3 months of today")}) " +
            "${yellow("with the following format:")} " +
            green("yyyy-MM-dd")
        )
      }
    } while (true)
  }

  private fun isValidDate(input: String): Boolean {
    return try {
      val date = parseSuppressUntilDate(input)
      date.isAfter(LocalDate.now()) && date.isBefore(LocalDate.now().plusMonths(MAX_SUPPRESSION_MONTHS))
    } catch (e: DateTimeParseException) {
      false
    }
  }

  private fun rejected(input: String): Boolean {
    return input == "n"
  }
}
