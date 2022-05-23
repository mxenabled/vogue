package com.mx.vogue

import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

class VoguePluginTest extends Specification {
  def "plugin registers task"() {
    when:
    def project = ProjectBuilder.builder().build()
    project.plugins.apply("com.mx.vogue")

    then:
    project.tasks.findByName("vogueReport") != null
  }
}
