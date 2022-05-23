package com.mx.vogue.core.models

import com.mx.vogue.core.VersionNumber

data class DependencyContext(
  val versionsPluginDependency: VersionsPluginDependency,
  var current: VersionNumber,
  var latest: VersionNumber,
  var maxMajorDiffExceeded: Boolean = false,
  var maxMinorDiffExceeded: Boolean = false,
  var maxMicroDiffExceeded: Boolean = false,
  var maxPatchVersionExceeded: Boolean = false,
  var majorUpgradeAvailable: Boolean = false,
  var minorUpgradeAvailable: Boolean = false,
  var microUpgradeAvailable: Boolean = false,
  var patchUpgradeAvailable: Boolean = false,
  var suppressUntil: String? = null
)

data class VogueReport(
  val upToDate: VersionsPluginItem,
  val outdated: MutableList<DependencyContext> = mutableListOf()
)
