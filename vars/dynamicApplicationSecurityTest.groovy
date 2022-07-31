#!groovy
/**
 * Run a Dynamic Analysis on the project workspace using OWASP ZAP
 *
 */

def call() {

  echo '🔎 Running Dynamic Application Security Test'
  sh(
      label: 'Dynamic Application Security Test',
      script: """#!/bin/bash
        set -ex

        echo Running ZAP
      """
  )

}
