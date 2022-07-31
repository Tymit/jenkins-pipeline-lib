#!groovy
/**
 * Run a Static Analysis on the project workspace using MobSFScan
 *
 *  Usage (e.g. in a pipeline script):
 *
 *     staticApplicationSecurityTestMobile()
 */

def call(Map parameters = [:]) {

  echo '🔎 Debug parameters'
  parameters.each{ k, v -> println "${k}:${v}" }

  echo '🔎 Running Static Analysis'
  sh(
      label: 'mobsfscan',
      script: """#!/bin/bash
        set -ex

        docker run \
        --pull=always \
        -v ${env.WORKSPACE}:/src \
        opensecurity/mobsfscan:0.1.0 \
        --sarif --output /src/sast-report.sarif /src || true

        sed -i'.bak' "s/file:\\/\\/\\/src\\///g" sast-report.sarif
      """
  )

  echo '📊 Archiving Static Analysis test results'
  archiveArtifacts artifacts: '**/sast-report.sarif', fingerprint: true

  if (parameters.get( 'isEnabledRecordIssues', true )) {
    echo '📊 Recording Static Analysis results'
    recordIssues(
      aggregatingResults: false,
      enabledForFailure: true,
      ignoreQualityGate: false,
      ignoreFailedBuilds: true,
      qualityGates: [
        [threshold: 1, type: 'TOTAL', unstable: true ]
      ],
      tool: sarif(
        name: 'Static Analysis',
        pattern:  'sast-report.sarif',
        reportEncoding:'UTF-8'
        )
    )
  }

}
