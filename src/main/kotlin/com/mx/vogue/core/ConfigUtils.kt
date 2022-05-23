package com.mx.vogue.core

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.mx.vogue.core.models.Configuration
import com.mx.vogue.core.models.PackageRule
import com.mx.vogue.core.models.Rules
import java.io.File
import java.io.FileNotFoundException

@Suppress("ReturnCount")
fun loadConfiguration(path: String): Configuration? {
  val file = File(path)
  if (!file.exists()) {
    return null
  }

  val mapper = ObjectMapper(YAMLFactory())

  val contents = file.readText(Charsets.UTF_8)
  if (contents.isEmpty()) {
    return null
  }

  return mapper.readValue(contents, Configuration::class.java)
}

fun writeConfiguration(path: String, config: Configuration) {
  val file = File(path)

  val mapper = ObjectMapper(YAMLFactory())
  mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
  file.writeBytes(mapper.writeValueAsBytes(config))
}

fun loadDefaultConfiguration(): Configuration {
  val stream = object {}.javaClass.classLoader.getResourceAsStream("default.vogue.yml")
    ?: throw FileNotFoundException("Default configuration file could not be loaded")

  val mapper = ObjectMapper(YAMLFactory())
  mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
  return mapper.readValue(stream.readBytes(), Configuration::class.java)
}

fun copySampleConfigurationToPath(path: String) {
  val stream = object {}.javaClass.classLoader.getResourceAsStream(".vogue.yml.sample")
    ?: throw FileNotFoundException("Sample configuration file could not be loaded")

  val file = File(path)

  if (!file.exists()) {
    file.writeBytes(stream.readBytes())
  }
}

fun mergeConfiguration(source: Configuration, overrides: Configuration?) {
  if (overrides == null) {
    return
  }

  mergeDefaultRules(source, overrides)
  mergePackageRules(source, overrides)
}

private fun mergeDefaultRules(source: Configuration, overrides: Configuration) {
  overrides.defaultRules?.let {
    if (source.defaultRules == null) {
      source.defaultRules = Rules()
    }

    mergeRules(source.defaultRules!!, it)
  }
}

private fun mergePackageRules(source: Configuration, overrides: Configuration) {
  overrides.packageRules?.let {
    if (source.packageRules == null) {
      source.packageRules = mutableListOf()
    }

    it.forEach { packageRule ->
      mergePackageRule(source.packageRules!!, packageRule)
    }
  }
}

private fun mergePackageRule(source: MutableList<PackageRule>, override: PackageRule) {
  val sourceRule = source.find { it.`package` == override.`package` }

  // If we can't find a rule to override, we add the rule to the beginning of the list
  // so that it will take precedence over any other rules that might have executed for the
  // given package.
  if (sourceRule == null) {
    source.add(0, override)
    return
  }

  override.rules?.let {
    if (sourceRule.rules == null) {
      sourceRule.rules = Rules()
    }
    mergeRules(sourceRule.rules!!, override.rules!!)
  }

  mergeSuppressUntil(sourceRule, override)
}

private fun mergeRules(source: Rules, overrides: Rules) {
  overrides.let {
    it.major?.let { source.major = overrides.major }
    it.minor?.let { source.minor = overrides.minor }
    it.micro?.let { source.micro = overrides.micro }
    it.patch?.let { source.patch = overrides.patch }
  }
}

private fun mergeSuppressUntil(source: PackageRule, override: PackageRule) {
  override.suppressUntil?.let {
    source.suppressUntil = it
  }
}
