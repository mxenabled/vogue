package com.mx.vogue.core

import static org.mockito.Mockito.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.mx.vogue.core.models.Available
import com.mx.vogue.core.models.Configuration
import com.mx.vogue.core.models.DependencyContext
import com.mx.vogue.core.models.VersionsPluginDependency

import spock.lang.Specification

class SuppressorTest extends Specification {
  Suppressor subject

  def setup() {
    subject = spy(new Suppressor())
  }

  def "Can opt out of dependency suppression"() {
    given:
    def config = new Configuration()
    def violations = new ArrayList<DependencyContext>().tap {
      add(makeDependencyContext("com.mx", "gateway", "1.0.2", "2.0.0"))
    }

    when:
    System.setIn(new ByteArrayInputStream("n".getBytes()))
    subject.suppressViaUserInput(config, violations)

    then:
    noExceptionThrown()
    config.packageRules.size() == 0
  }

  def "Can suppress a dependency"() {
    given:
    def config = new Configuration()
    def violations = new ArrayList<DependencyContext>().tap {
      add(makeDependencyContext("com.mx", "gateway", "1.0.2", "2.0.0"))
    }

    when:
    System.setIn(new ByteArrayInputStream("y\ny".getBytes()))
    subject.suppressViaUserInput(config, violations)

    then:
    noExceptionThrown()
    config.packageRules.size() == 1
    config.packageRules[0].package == "com.mx:gateway"
  }

  def "Can suppress a dependency with custom suppression date"() {
    given:
    def config = new Configuration()
    def violations = new ArrayList<DependencyContext>().tap {
      add(makeDependencyContext("com.mx", "gateway", "1.0.2", "2.0.0"))
    }
    def customSuppressUntil = LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    when:
    System.setIn(new ByteArrayInputStream("y\n${customSuppressUntil}".getBytes()))
    subject.suppressViaUserInput(config, violations)

    then:
    noExceptionThrown()
    config.packageRules.size() == 1
    config.packageRules[0].package == "com.mx:gateway"
    config.packageRules[0].suppressUntil == customSuppressUntil
  }

  def "Can opt out of suppressing a dependency after opting in"() {
    given:
    def config = new Configuration()
    def violations = new ArrayList<DependencyContext>().tap {
      add(makeDependencyContext("com.mx", "gateway", "1.0.2", "2.0.0"))
    }

    when:
    System.setIn(new ByteArrayInputStream("y\nn".getBytes()))
    subject.suppressViaUserInput(config, violations)

    then:
    noExceptionThrown()
    config.packageRules.size() == 0
  }

  private DependencyContext makeDependencyContext(String group, String name, String current, String latest) {
    return new DependencyContext(
        new VersionsPluginDependency(group, current, new Available(latest, "", ""), name),
        VersionNumber.parse(current),
        VersionNumber.parse(latest),
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        null )
  }
}
