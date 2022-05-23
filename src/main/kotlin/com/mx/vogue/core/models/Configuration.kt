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

import com.fasterxml.jackson.annotation.JsonProperty

data class Configuration(
  @JsonProperty("defaultRules")
  var defaultRules: Rules? = Rules(),
  @JsonProperty("packageRules")
  var packageRules: MutableList<PackageRule>? = mutableListOf()
)

data class PackageRule(
  // Jackson has a heck-of-a-time trying to serialize this correctly if we don't help it out by naming this
  // "package" explicitly.
  @Suppress("ConstructorParameterNaming")
  @JsonProperty("package")
  var `package`: String = "",
  @JsonProperty("rules")
  var rules: Rules? = Rules(),
  @JsonProperty("suppressUntil")
  var suppressUntil: String? = null, // yyyy-MM-dd or yyyy/MM/dd
  @JsonProperty("projectIssue")
  var projectIssue: String? = null
)

class Rules(
  @JsonProperty("major")
  var major: Rule? = null,
  @JsonProperty("minor")
  var minor: Rule? = null,
  // Gradle calls this a micro version, but SemVer calls it a patch version. We'll expose
  // it as a patch version, but internally call it a micro version to keep things consistent.
  @JsonProperty("patch")
  var micro: Rule? = null,
  // We'll call this the micro version externally, but internally refer to it as the patch version.
  @JsonProperty("micro")
  var patch: Rule? = null
)

data class Rule(
  @JsonProperty("maxDiff")
  var maxDiff: Int = -1,
  @JsonProperty("requireLatest")
  var requireLatest: Boolean = false
)
