package com.mx.vogue.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.mx.vogue.core.exceptions.VogueProcessingException
import com.mx.vogue.core.models.PackageRule

import spock.lang.Specification

class ReportUtilsTest extends Specification {
  def "reportStaleSuppressions ignores non-stale and null suppressions"() {
    when:
    ReportUtilsKt.reportStaleSuppressions(new ArrayList<PackageRule>().tap {
      add(new PackageRule().tap {
        it.package = "com.mx.web"
        suppressUntil = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
      })
      add(new PackageRule().tap {
        it.package = "io.commons"
        suppressUntil = LocalDate.now().plusDays(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
      })
      add(new PackageRule().tap {
        it.package = "something.else"
        suppressUntil = null
      })
    })

    then:
    noExceptionThrown()
  }

  def "reportStaleSuppressions reports stale suppressions"() {
    given:
    def fiveDaysAgo = LocalDate.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    def sixDaysAhead = LocalDate.now().plusDays(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    when:
    ReportUtilsKt.reportStaleSuppressions(new ArrayList<PackageRule>().tap {
      add(new PackageRule().tap {
        it.package = "com.mx.web"
        suppressUntil = fiveDaysAgo
      })
      add(new PackageRule().tap {
        it.package = "io.commons"
        suppressUntil = sixDaysAhead
      })
      add(new PackageRule().tap {
        it.package = "something.else"
        suppressUntil = null
      })
    })

    then:
    def e = thrown(VogueProcessingException)
    e.message.contains("The following suppressions have gone stale. Please either remove them from the .vogue.yml or update the suppressUntil date.")
    e.message.contains("com.mx.web")
    e.message.contains(fiveDaysAgo)
    !e.message.contains("io.commons")
    !e.message.contains(sixDaysAhead)
    !e.message.contains("something.else")
  }

  def "extractStaleSuppressions"() {
    given:
    def fiveDaysAgo = LocalDate.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    def sixDaysAhead = LocalDate.now().plusDays(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    when:
    def suppressions = ReportUtilsKt.filterStaleSuppressions(new ArrayList<PackageRule>().tap {
      add(new PackageRule().tap {
        it.package = "com.mx.web"
        suppressUntil = fiveDaysAgo
      })
      add(new PackageRule().tap {
        it.package = "io.commons"
        suppressUntil = sixDaysAhead
      })
      add(new PackageRule().tap {
        it.package = "something.else"
        suppressUntil = null
      })
    })

    then:
    suppressions.size() == 2
    suppressions[0].package == "io.commons"
    suppressions[1].package == "something.else"
  }
}
