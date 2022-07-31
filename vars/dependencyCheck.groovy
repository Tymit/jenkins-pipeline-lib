#!groovy
/**
 * Run a Dependency Check on the project workspace using OWASP Dependency Check
 *
 *  Usage (e.g. in a pipeline script):
 *
 *     dependencyCheck()
 *
 */

def call() {

  echo '🔎 Running OWASP Dependency Check'
  sh(
      label: 'Dependency Check',
      script: """#!/bin/bash
        set -ex

        docker volume create --name dependency-check-cache
        docker run \
          --pull=always \
          -e user=\$(id -u) \
          -v ${env.WORKSPACE}:/src \
          -v dependency-check-cache:/usr/share/dependency-check/data \
          owasp/dependency-check:latest \
          --scan /src --format "ALL"
      """
  )

  // echo '📊 Archiving Static Analysis results'
  // junit checksName: 'Semgrep results',
  //       allowEmptyResults: true,
  //       testResults: '**/junit-sast-report.xml',
  //       skipPublishingChecks: true

  // echo '📊 Recording static analysis results'
  // recordIssues(
  //     enabledForFailure: true, aggregatingResults: true,
  //     tool: junitParser(pattern:  'junit-sast-report.xml ',
  //                         reportEncoding:'UTF-8',
  //                         name: 'Static Analysis Results')
  // )

}
