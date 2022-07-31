#!groovy
/**
 * Run a Static Analysis on the project workspace using Semgrep
 *
 * Parameters:
 *
 *     `rules` (optional): the rule groups to use in the scan.
 *
 *  Usage (e.g. in a pipeline script):
 *
 *     staticApplicationSecurityTest()
 *     staticApplicationSecurityTest(rules: 'quarkus')
 *
 * N.B. Rules can be one of 'java', 'quarkus', 'python', 'flask' 'nodejs',
 *     'typescript', 'react'
 */

def call(Map parameters = [:]) {
  // If not specified use the default rule group.
  String rules = parameters.get('rules', 'default')

  List defaultRules = ['p/ci', 'p/owasp-top-ten', 'p/security-audit', 'p/jwt', 'p/xss']
  List baseJavaRules = ['p/java']
  List baseNodeRules = ['p/nodejs', 'p/nodejsscan', 'p/eslint-plugin-security']
  List basePythonRules = ['p/python']
  List baseFlaskRules = ['p/flask']
  List baseReactRules = ['p/react']
  List baseTypeScriptRules = ['p/typescript']
  String semgrepRules

  switch (rules) {
    case ['java', 'quarkus']:
      semgrepRules = (defaultRules + baseJavaRules).join(' ')
      break
    case 'python':
      semgrepRules = (defaultRules + basePythonRules).join(' ')
      break
    case 'flask':
      semgrepRules = (defaultRules + basePythonRules + baseFlaskRules).join(' ')
      break
    case 'nodejs':
      semgrepRules = (defaultRules + baseNodeRules).join(' ')
      break
    case 'typescript':
      semgrepRules = (defaultRules + baseNodeRules + baseTypeScriptRules).join(' ')
      break
    case 'react':
      semgrepRules = (defaultRules + baseNodeRules + baseTypeScriptRules + baseReactRules).join(' ')
      break
    case 'default':
      semgrepRules = defaultRules.join(' ')
      break
  }

  echo '🔎 Running Static Analysis'
  sh(
      label: 'Semgrep',
      script: """#!/bin/bash
        set -ex

        docker run \
        --pull=always \
        -e 'SEMGREP_RULES=${semgrepRules}' \
        -v ${env.WORKSPACE}:/src \
        returntocorp/semgrep:latest \
        semgrep scan --disable-version-check --no-git-ignore --metrics=off --no-error --sarif -o sast-report.sarif
      """
  )

  echo '📊 Archiving Static Analysis test results'
  archiveArtifacts artifacts: '**/sast-report.sarif', fingerprint: true
  // junit checksName: 'Static Analysis',
  //       allowEmptyResults: true,
  //       testResults: '**/sast-report.xml',
  //       skipPublishingChecks: true

  echo '📊 Recording Static Analysis results'
  recordIssues(
      aggregatingResults: false,
      enabledForFailure: true,
      ignoreQualityGate: false,
      ignoreFailedBuilds: true,
      qualityGates: [
        [threshold: 1, type: 'TOTAL_ERROR', unstable: true],
        [threshold: 1, type: 'TOTAL_HIGH', unstable: true]
      ],
      tool: sarif(
        name: 'Static Analysis',
        pattern:  'sast-report.sarif',
        reportEncoding:'UTF-8'
        )
  )
}
