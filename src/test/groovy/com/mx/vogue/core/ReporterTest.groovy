package com.mx.vogue.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.mx.vogue.core.exceptions.VogueProcessingException
import com.mx.vogue.core.models.Configuration
import com.mx.vogue.core.models.PackageRule
import com.mx.vogue.core.models.Rule
import com.mx.vogue.core.models.Rules

import spock.lang.Specification

class ReporterTest extends Specification {
  Reporter subject

  def setup() {
    subject = new Reporter(new Configuration().tap {
      defaultRules = new Rules().tap {
        major = new Rule().tap { maxDiff = 0 }
        minor = new Rule().tap { maxDiff = 0 }
        micro = new Rule().tap { maxDiff = 0 }
      }
    })
  }

  def "loads and parses a gradle-versions-plugin report"() {
    when:
    subject.generate("src/test/resources/report.json")

    then:
    noExceptionThrown()
  }

  def "throws an exception when the report cannot be found"() {
    when:
    subject.generate("src/test/resources/foo.json")

    then:
    thrown(FileNotFoundException)
  }

  def "generates a report based on the supplied rules"() {
    when:
    def report = subject.generate("src/test/resources/report.json")

    then:
    report.outdated.size() == 3
    report.outdated[0].current.toString() == "5.2.1"
    report.outdated[0].latest.toString() == "6.3.0"
    report.outdated[0].maxMajorDiffExceeded
    !report.outdated[0].maxMinorDiffExceeded
    !report.outdated[0].maxMicroDiffExceeded
    !report.outdated[0].maxPatchVersionExceeded
    report.outdated[0].majorUpgradeAvailable
    !report.outdated[0].minorUpgradeAvailable
    !report.outdated[0].microUpgradeAvailable
    !report.outdated[0].patchUpgradeAvailable

    report.outdated[1].current.toString() == "1.0.10-RELEASE"
    report.outdated[1].latest.toString() == "1.0.11-RELEASE"
    !report.outdated[1].maxMajorDiffExceeded
    !report.outdated[1].maxMinorDiffExceeded
    report.outdated[1].maxMicroDiffExceeded
    !report.outdated[1].maxPatchVersionExceeded
    !report.outdated[1].majorUpgradeAvailable
    !report.outdated[1].minorUpgradeAvailable
    report.outdated[1].microUpgradeAvailable
    !report.outdated[1].patchUpgradeAvailable

    report.outdated[2].current.toString() == "1.7.30"
    report.outdated[2].latest.toString() == "1.18.2"
    !report.outdated[2].maxMajorDiffExceeded
    report.outdated[2].maxMinorDiffExceeded
    !report.outdated[2].maxMicroDiffExceeded
    !report.outdated[2].maxPatchVersionExceeded
    !report.outdated[2].majorUpgradeAvailable
    report.outdated[2].minorUpgradeAvailable
    !report.outdated[2].microUpgradeAvailable
    !report.outdated[2].patchUpgradeAvailable

    report.upToDate.count == 2
    report.upToDate.dependencies[0].group == "com.mx.coppuccino"
    report.upToDate.dependencies[1].group == "com.mx.path.api.connect"
  }

  def "applies a package-specific rule if one is supplied"() {
    given:
    subject = new Reporter(new Configuration().tap {
      defaultRules = new Rules()
      packageRules = new ArrayList<>().tap {
        add(buildPackageRule("io.spring.dependency-management"))
      }
    })

    when:
    def report = subject.generate("src/test/resources/report.json")

    then:
    report.outdated.size() == 3
    report.outdated[0].current.toString() == "5.2.1"
    report.outdated[0].latest.toString() == "6.3.0"
    !report.outdated[0].maxMajorDiffExceeded
    !report.outdated[0].maxMinorDiffExceeded
    !report.outdated[0].maxMicroDiffExceeded
    !report.outdated[0].maxPatchVersionExceeded

    report.outdated[1].current.toString() == "1.0.10-RELEASE"
    report.outdated[1].latest.toString() == "1.0.11-RELEASE"
    !report.outdated[1].maxMajorDiffExceeded
    !report.outdated[1].maxMinorDiffExceeded
    report.outdated[1].maxMicroDiffExceeded
    !report.outdated[1].maxPatchVersionExceeded

    report.outdated[2].current.toString() == "1.7.30"
    report.outdated[2].latest.toString() == "1.18.2"
    !report.outdated[2].maxMajorDiffExceeded
    !report.outdated[2].maxMinorDiffExceeded
    !report.outdated[2].maxMicroDiffExceeded
    !report.outdated[2].maxPatchVersionExceeded
  }

  def "can suppress a packageRules violation"() {
    given:
    def suppressUntil = LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    subject = new Reporter(new Configuration().tap {
      defaultRules = new Rules()
      packageRules = new ArrayList<>().tap {
        // The suppression override.
        add(buildPackageRule("io.freefair.lombok", suppressUntil))
        // The original rule, hella strict.
        add(buildPackageRule("io.freefair.lombok"))
      }
    })

    when:
    def report = subject.generate("src/test/resources/report.json")

    then:
    report.outdated[0].current.toString() == "5.2.1"
    report.outdated[0].latest.toString() == "6.3.0"
    report.outdated[0].suppressUntil == suppressUntil
    !report.outdated[0].maxMajorDiffExceeded
    !report.outdated[0].maxMinorDiffExceeded
    !report.outdated[0].maxMicroDiffExceeded
    !report.outdated[0].maxPatchVersionExceeded
  }

  def "can suppress a defaultRules violation"() {
    given:
    def suppressUntil = LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    subject = new Reporter(new Configuration().tap {
      defaultRules = new Rules().tap {
        major = new Rule().tap {
          requireLatest = true
        }
      }
      packageRules = new ArrayList<>().tap {
        add(buildPackageRule("io.freefair.lombok", suppressUntil))
      }
    })

    when:
    def report = subject.generate("src/test/resources/report.json")

    then:
    report.outdated[0].current.toString() == "5.2.1"
    report.outdated[0].latest.toString() == "6.3.0"
    report.outdated[0].suppressUntil == suppressUntil
    !report.outdated[0].maxMajorDiffExceeded
    !report.outdated[0].maxMinorDiffExceeded
    !report.outdated[0].maxMicroDiffExceeded
    !report.outdated[0].maxPatchVersionExceeded
  }

  def "outdated suppression returns violations"() {
    given:
    subject = new Reporter(new Configuration().tap {
      defaultRules = new Rules()
      packageRules = new ArrayList<>().tap {
        add(buildPackageRule("io.freefair.lombok", LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
      }
    })

    when:
    def report = subject.generate("src/test/resources/report.json")

    then:
    report.outdated[0].current.toString() == "5.2.1"
    report.outdated[0].latest.toString() == "6.3.0"
    report.outdated[0].maxMajorDiffExceeded
    !report.outdated[0].maxMinorDiffExceeded
    !report.outdated[0].maxMicroDiffExceeded
    !report.outdated[0].maxPatchVersionExceeded
  }

  def "throws an error if 'suppressUntil' is incorrectly formatted"() {
    given:
    subject = new Reporter(new Configuration().tap {
      defaultRules = new Rules()
      packageRules = new ArrayList<>().tap {
        add(buildPackageRule("io.freefair.lombok", "02+22+2022"))
      }
    })

    when:
    def report = subject.generate("src/test/resources/report.json")

    then:
    def e = thrown(VogueProcessingException)
    e.message.contains("Invalid 'suppressUntil' date provided: 02+22+2022.")
  }

  def "throws an error if 'suppressUntil' is a date too far into the future"() {
    given:
    subject = new Reporter(new Configuration().tap {
      defaultRules = new Rules()
      packageRules = new ArrayList<>().tap {
        add(buildPackageRule("io.freefair.lombok", LocalDate.now().plusMonths(ReportUtilsKt.MAX_FUTURE_DATE_MONTHS).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
      }
    })

    when:
    def report = subject.generate("src/test/resources/report.json")

    then:
    def e = thrown(VogueProcessingException)
    e.message.contains("Invalid 'suppressUntil' date provided:")
    e.message.contains("The date must be within $ReportUtilsKt.MAX_FUTURE_DATE_MONTHS months of today.")
  }

  private PackageRule buildPackageRule(String pkg) {
    return buildPackageRule(pkg, null)
  }

  private PackageRule buildPackageRule(String pkg, String suppressUntil) {
    return new PackageRule().tap {
      it.package = pkg
      it.suppressUntil = suppressUntil
      projectIssue = "https://github.com/mxenabled/vogue/issues/1"
      rules = new Rules().tap {
        major = new Rule().tap { requireLatest = true }
        minor = new Rule().tap { requireLatest = true }
        micro = new Rule().tap { requireLatest = true }
      }
    }
  }
}
