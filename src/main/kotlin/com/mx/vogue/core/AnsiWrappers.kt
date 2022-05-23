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
