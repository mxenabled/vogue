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
