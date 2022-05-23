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

private const val ESC = "\u001B"
private const val CYAN =
  """$ESC[36m"""
private const val RED =
  """$ESC[31m"""
private const val GREEN =
  """$ESC[32m"""
private const val YELLOW =
  """$ESC[33m"""
private const val NO_COLOR =
  """$ESC[0m"""

fun cyan(text: String): String {
  return "$CYAN$text$NO_COLOR"
}

fun red(text: String): String {
  return "$RED$text$NO_COLOR"
}

fun green(text: String): String {
  return "$GREEN$text$NO_COLOR"
}

fun yellow(text: String): String {
  return "$YELLOW$text$NO_COLOR"
}
