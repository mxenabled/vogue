package com.mx.vogue.core

import com.mx.vogue.core.models.Configuration
import com.mx.vogue.core.models.Rule
import com.mx.vogue.core.models.Rules

import spock.lang.Specification

class ReportRendererTest extends Specification {
  def "renders a VogueReport into a string"() {
    given:
    def report = new Reporter(new Configuration().tap {
      defaultRules = new Rules(
          new Rule(1000, false),
          new Rule(0, false),
          new Rule(0, false),
          new Rule(0, false)
          )
    }).generate("src/test/resources/report.json")

    when:
    def result = ReportRendererKt.renderReport(report)

    then:
    result == new String(new File("src/test/resources/renderedReport.txt").readBytes())
  }
}
