package com.mx.vogue.core

import com.mx.vogue.core.models.Configuration
import com.mx.vogue.core.models.PackageRule
import com.mx.vogue.core.models.Rule
import com.mx.vogue.core.models.Rules

import spock.lang.Specification

class ConfigUtilsTest extends Specification {
  def "loadConfiguration"() {
    given:
    def path = "src/test/resources/.vogue.yml"

    when:
    def configuration = ConfigUtilsKt.loadConfiguration(path)

    then:
    configuration.defaultRules.major.maxDiff == 1
    configuration.defaultRules.minor.maxDiff == 2

    configuration.packageRules.size() == 2
    configuration.packageRules[0].package == "com.mx.*"
    configuration.packageRules[0].rules.major.requireLatest
    configuration.packageRules[0].rules.minor.maxDiff == 2
    configuration.packageRules[0].rules.micro.maxDiff == 3

    configuration.packageRules[1].package == "com.google.code.gson.*"
    configuration.packageRules[1].rules.major.maxDiff == 1
    configuration.packageRules[1].rules.micro.maxDiff == 0
  }

  def "mergeConfiguration (overrides all defaultRules)"() {
    given:
    def defaultConfig = new Configuration().tap {
      defaultRules = new Rules().tap {
        major = new Rule().tap {maxDiff = 1 }
        minor = new Rule().tap {requireLatest = true }
        micro = new Rule().tap {requireLatest = true }
      }
    }

    def overrides = new Configuration().tap {
      defaultRules = new Rules().tap {
        major = new Rule().tap {maxDiff = 10 }
        minor = new Rule().tap {maxDiff = 9 }
        micro = new Rule().tap {maxDiff = 8 }
      }
    }

    when:
    ConfigUtilsKt.mergeConfiguration(defaultConfig, overrides)

    then:
    defaultConfig.defaultRules.major.maxDiff == 10
    !defaultConfig.defaultRules.major.requireLatest

    defaultConfig.defaultRules.minor.maxDiff == 9
    !defaultConfig.defaultRules.minor.requireLatest

    defaultConfig.defaultRules.micro.maxDiff == 8
    !defaultConfig.defaultRules.micro.requireLatest
  }

  def "mergeConfiguration (overrides single defaultRule)"() {
    given:
    def defaultConfig = new Configuration().tap {
      defaultRules = new Rules().tap {
        major = new Rule().tap {maxDiff = 1}
        minor = new Rule().tap {requireLatest = true}
        micro = new Rule().tap {requireLatest = true}
      }
    }

    def overrides = new Configuration().tap {
      defaultRules = new Rules().tap {
        micro = new Rule().tap {maxDiff = 8}
      }
    }

    when:
    ConfigUtilsKt.mergeConfiguration(defaultConfig, overrides)

    then:
    defaultConfig.defaultRules.major.maxDiff == 1
    !defaultConfig.defaultRules.major.requireLatest

    defaultConfig.defaultRules.minor.maxDiff == -1
    defaultConfig.defaultRules.minor.requireLatest

    defaultConfig.defaultRules.micro.maxDiff == 8
    !defaultConfig.defaultRules.micro.requireLatest
  }

  def "mergeConfiguration (overrides a PackageRule)"() {
    given:
    def defaultConfig = new Configuration().tap {
      packageRules = new ArrayList<PackageRule>().tap {
        add(new PackageRule().tap {
          it.package = "com.mx.*"
          rules = new Rules().tap {
            major = new Rule().tap {requireLatest = true}
            minor = new Rule().tap {requireLatest = true}
            micro = new Rule().tap {requireLatest = true}
          }
        })
      }
    }

    def overrides = new Configuration().tap {
      packageRules = new ArrayList<PackageRule>().tap {
        add(new PackageRule().tap {
          it.package = "com.mx.*"
          suppressUntil = "2022/10/31" // Spooky
          rules = new Rules().tap {
            major = new Rule().tap {maxDiff = 200}
            minor = new Rule().tap {maxDiff = 100}
          }
        })
      }
    }

    when:
    ConfigUtilsKt.mergeConfiguration(defaultConfig, overrides)

    then:
    defaultConfig.packageRules.size() == 1
    defaultConfig.packageRules[0].package == "com.mx.*"
    defaultConfig.packageRules[0].suppressUntil == "2022/10/31"

    defaultConfig.packageRules[0].rules.major.maxDiff == 200
    !defaultConfig.packageRules[0].rules.major.requireLatest

    defaultConfig.packageRules[0].rules.minor.maxDiff == 100
    !defaultConfig.packageRules[0].rules.minor.requireLatest

    defaultConfig.packageRules[0].rules.micro.maxDiff == -1
    defaultConfig.packageRules[0].rules.micro.requireLatest
  }

  def "mergeConfiguration (prepends a PackageRule)"() {
    given:
    def defaultConfig = new Configuration().tap {
      packageRules = new ArrayList<PackageRule>().tap {
        add(new PackageRule().tap {
          it.package = "com.mx.*"
          rules = new Rules().tap {
            major = new Rule().tap {requireLatest = true}
            minor = new Rule().tap {requireLatest = true}
            micro = new Rule().tap {requireLatest = true}
          }
        })
      }
    }

    def overrides = new Configuration().tap {
      packageRules = new ArrayList<PackageRule>().tap {
        add(new PackageRule().tap {
          it.package = "com.mx.web"
          rules = new Rules().tap {
            major = new Rule().tap {maxDiff = 200}
            minor = new Rule().tap {maxDiff = 100}
          }
        })
      }
    }

    when:
    ConfigUtilsKt.mergeConfiguration(defaultConfig, overrides)

    then:
    defaultConfig.packageRules.size() == 2
    defaultConfig.packageRules[0].package == "com.mx.web"
    defaultConfig.packageRules[0].suppressUntil == null

    defaultConfig.packageRules[0].rules.major.maxDiff == 200
    !defaultConfig.packageRules[0].rules.major.requireLatest

    defaultConfig.packageRules[0].rules.minor.maxDiff == 100
    !defaultConfig.packageRules[0].rules.minor.requireLatest

    defaultConfig.packageRules[0].rules.micro == null

    defaultConfig.packageRules[1].package == "com.mx.*"
  }
}
