#!groovy

// ============================================================================
// Jenkins   Pipeline
// https://jenkins.io/doc/book/pipeline/syntax/
// ============================================================================

// ============================================================================
// Shared Libraries
// https://www.jenkins.io/doc/book/pipeline/shared-libraries/
// Note: see vars/ directory in this repo for Shared Library code
// ============================================================================

// import a preconfigured shared library to use its functions for code reuse
//@Library(value="name@version", changelog=true|false)
// changelog will not only include lib changes in changeset but also trigger new
// builds when the library changes, not very scalable to auto-re-run all pipelines
//@Library('namedlibrary@master') _
// @Library(value='Utils@develop', changelog=false) _
@Library('Utils@PLAT-X_more_stepz') _

// more dynamic but $BRANCH_NAME is only available in a Jenkins MultiBranch Pipeline
//library "namedlibrary@$BRANCH_NAME"

String targetChannel = "#xblue"
String agentLabel = "platform"
String containerLabel = "722057856351.dkr.ecr.eu-west-1.amazonaws.com/platform/platform-cli:develop-latest"

pipeline {
    // ======================================================================
    // Agents
    // ======================================================================

    // agent setting is required otherwise build won't ever run
    // run pipeline on any agent
    // agent any
    //
    // can override this on a per stage basis, but leaving this as "any" will
    // incur some overhead on the Jenkins master in that case, better to switch
    // whole pipeline to a remote agent if you can, unless you want to parallelize
    // the stages among different agents, which might be especially useful for
    // on-demand cloud agents run in Containers
    // putting agents{} sections in stage will also have options applied to the
    // agent eg. timeout includes agent provisioning time
    agent { label agentLabel }

    // ======================================================================
    // Options
    // ======================================================================
    options {
        // requires ansicolor plugin
        ansiColor('xterm')

        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10', daysToKeepStr:'10'))

        // only allow 1 of this pipeline to run at a time and  optionally
        // abort previous build if running
        //disableConcurrentBuilds()
        disableConcurrentBuilds(abortPrevious: true)

        disableResume()

        durabilityHint('PERFORMANCE_OPTIMIZED')

        // https://www.jenkins.io/doc/book/pipeline/syntax/#parallel
        parallelsAlwaysFailFast()

        // skipStagesAfterUnstable()

        // timeout entire pipeline after 30 minutes
        timeout(time: 30, unit: 'MINUTES')

        // put timestamps in console logs
        timestamps()
    }

    // ======================================================================
    // Parameters
    // ======================================================================
    parameters {
        booleanParam(
            name: 'deploy',
            defaultValue: false,
            description: '🚧  Automatically deploy?')
        string(
            name: 'HUMAN',
            defaultValue: 'none',
            description: '👤 Who should I say hello to?')
        booleanParam(
            name: 'skipLinter',
            defaultValue: true,
            description: '🔎 Skip Linter?')
        booleanParam(
            name: 'skipStaticAnalysis',
            defaultValue: false,
            description: '🔎 Skip Static Analysis?')
        booleanParam(
            name: 'skipDependencyCheck',
            defaultValue: true,
            description: '🔎 Skip Dependency Check?')
        booleanParam(
            name: 'tagRelease',
            defaultValue: false,
            description: "Tag this release commit")
    }

  // ========================================================================== //
  // Environment & Credentials
  // ========================================================================== //
  //    https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#handling-credentials
  //
  // Do not allow untrusted Pipeline jobs / users to use trusted Credentials
  // as they can extract these environment variables
  //
  // these will be starred out *** in console log but user scripts can still print these
  // can move this under a stage to limit the scope of their visibility
  environment {
    ENV = 'dev'

    // Can be used by Git branch merges and tags
    GIT_USERNAME = 'Jenkins'
    GIT_EMAIL = 'platform-engineering@tymit.com'

    // general git command debugging
    //GIT_TRACE=1
    //GIT_TRACE_SETUP=1

    // should store these in Git for security and auditing purposes before loading to Jenkins credentials
    // BITBUCKET_SSH_KNOWN_HOSTS = credentials('bitbucket-ssh-known-hosts')        // eg. 'ssh-keyscan bitbucket.org'

    // create these credentials as Secret Text in Jenkins UI -> Manage Jenkins -> Manage Credentials -> Jenkins -> Global Credentials -> Add Credentials
    //AWS_ACCESS_KEY_ID      = credentials('aws-secret-key-id')
    //AWS_SECRET_ACCESS_KEY  = credentials('aws-secret-access-key')
    //AWS_ACCOUNT_ID = credentials('aws-account-id') // or better yet just generate it from access keys via 'aws sts get-caller-identity | jq -r .Account'
    //AWS_DEFAULT_REGION = 'eu-west-2'
    //AWS_ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
    //AWS_DEFAULT_OUTPUT = 'json'

    // if creating docker images on agents, this enables BuildKit which
    // automatically creates images layers in parallel where possible
    // (especially useful for multi-stage builds)
    // also add '--build-arg BUILDKIT_INLINE_CACHE=1' to the docker build command
    DOCKER_BUILDKIT = 1

    TF_IN_AUTOMATION = 1  // changes output to suppress CLI suggestions for related commands

    // using this only to dedupe common message suffix for Slack channel notifications in post {}
    SLACK_MESSAGE = "Pipeline <${env.JOB_DISPLAY_URL}|${env.JOB_NAME}> - <${env.RUN_DISPLAY_URL}|Build #${env.BUILD_NUMBER}>"
  }


  // ==========================================================================
  // Stages
  // ==========================================================================
  // https://www.jenkins.io/doc/pipeline/steps/
  // https://www.jenkins.io/doc/pipeline/steps/workflow-basic-steps/

    stages {
        stage('🌊 Cleanup') {
            agent none
            steps {
                echo 'We can use this stage to clean up resources'

                // Use this function or disableConcurrentBuilds(abortPrevious: true) in options
                // abortPreviousBuilds()
            }
        }

        // not usually needed when sourcing Jenkinsfile from Git SCM in
        // Pipeline / Multibranch Pipeline this is implied
        stage("💻 Checkout") {
            steps {
                checkout scm
                printEnv()  // function in /vars shared lib

                // Clone example Damn Vulnerable Java Application
                dir('dvja') {
                    git branch: 'master', url: 'https://github.com/appsecco/dvja.git'
                }
            }
        }

        stage('🕵 Code Analysis') {
            parallel {
                stage('Linter') {
                    when {
                        expression { return !params.skipLinter }
                    }
                    steps {
                        echo('🔎 Running Linter')
                        sh '''
                            #!/usr/bin/env bash -xe
                            echo make lint || (echo "Linter failed" && exit 0)
                        '''
                        }
                    }

                stage('Static Analysis') {
                    when {
                        expression { return !params.skipStaticAnalysis }
                    }
                    steps {
                        staticApplicationSecurityTest(rules: 'java')
                    }
                }

                stage('Dependency Check') {
                    when {
                        expression { return !params.skipDependencyCheck }
                    }
                    steps {
                        dependencyCheck()
                    }
                }
            }
        }

        stage('🛠️ Build') {
            steps {
                script {
                    echo 'Branch:       ' + env.BRANCH_NAME
                    echo 'Build:        ' + env.BUILD_NUMBER
                    echo 'JobName:      ' + env.JOB_NAME
                    echo 'JobBaseName:  ' + env.JOB_BASE_NAME
                    def timestamp = getTimeStamp()
                    echo 'getTimeStamp: ' + timestamp
                    def lastcommit = getLastCommit()
                    echo 'getLastCommit:' + lastcommit

                    def USERNAME = params.HUMAN != 'none' ? params.HUMAN : env.BUILD_NUMBER
                    sh """
                        echo 🖖 Hello Mr. ${USERNAME}
                    """
                }
            }
        }

        stage('👷 Code Tests') {
            when {
                anyOf {
                    branch 'develop';
                    branch 'staging';
                    branch 'master';
                    changeRequest()
                    expression {
                    // // True for pull requests, false otherwise.
                     env.CHANGE_ID && env.BRANCH_NAME.startsWith("PR-")
                    }
                }
            }
            steps {
                echo 'Testing Here'

                // JUnit reports
                script {
                    def xml =  '''
                        <testsuite tests="3">
                            <testcase classname="foo1" name="bar1"/>
                            <testcase classname="foo2" name="bar2"/>
                            <testcase classname="foo3" name="bar3">
                            </testcase>
                        </testsuite>
                    '''
                    writeFile file:"test.xml", text: xml

                    echo '📊 Archiving JUnit tests results'
                    junit checksName: 'Tests results',
                        allowEmptyResults: true,
                        testResults: '**/test.xml',
                        skipPublishingChecks: true
                    }
            }
        }

        stage('🏠 Deploy to playground') {
            when {
                anyOf { branch 'develop'; branch 'staging'; branch 'master' }
            }
            steps {
                script {
                    echo 'Deploy to playground'
                    slack.sendReport('DEPLOY',targetChannel,"Build ${currentBuild.displayName} successfully deployed to non-prod")
                }
            }
        }

        stage('🚧 Approval non-prod') {
            when {
                anyOf { branch 'develop'; branch 'staging'; branch 'master' }
                not {
                    equals expected: true, actual: params.deploy
                }
            }
            agent none
            steps {
                script {
                    try {
                        timeout(time: 1, unit: 'MINUTES') {
                            // you can use the commented line if u have specific user group who CAN ONLY approve
                            //input message:'Proceed to deployment?', submitter: 'authorized-user'
                            input message: 'Deploy to non-prod?', ok: 'Yes'
                        }
                    } catch (err) {
                        slack.sendReport('ABORTED',targetChannel,"Build Discarded: ${env.JOB_NAME} - ${env.BUILD_NUMBER}")
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('🏠 Deploy to nonprod') {
            when {
                anyOf { branch 'develop'; branch 'staging'; branch 'master' }
            }
            steps {
                script {
                    echo 'Deploy to non-prod'
                    slack.sendReport('DEPLOY',targetChannel,"Build ${currentBuild.displayName} successfully deployed to non-prod")
                }
            }
        }

        stage('🎬 E2E Testing') {
            when {
                anyOf { branch 'develop'; branch 'staging'; branch 'master' }
            }
            steps {
                echo 'E2E Testing Here'
                // Archive JUnit reports
                script {
                    def xml =  '''
                        <testsuite tests="3">
                            <testcase classname="foo1" name="bar1"/>
                            <testcase classname="foo2" name="bar2"/>
                            <testcase classname="foo3" name="bar3">
                                <failure type="nullpointer"> NULL </failure>
                                <system-out>beep beep ERROR beep beep</system-out>
                            </testcase>
                        </testsuite>
                    '''
                    writeFile file:"test.xml", text: xml

                    echo '📊 Archiving JUnit tests results'
                    junit checksName: 'E2E Tests results',
                        allowEmptyResults: true,
                        testResults: '**/test.xml',
                        skipPublishingChecks: true
                }
            }
        }

        stage('🚧 Approval prod') {
            when {
                branch 'master'
                not {
                    equals expected: true, actual: params.deploy
                }
            }
            agent none
            steps {
                script {
                    try {
                        timeout(time: 1, unit: 'MINUTES') {
                            // you can use the commented line if u have specific user group who CAN ONLY approve
                            //input message:'Proceed to deployment?', submitter: 'authorized-user'
                            input message: 'Deploy to prod?', ok: 'Yes'
                        }
                    } catch (err) {
                        slack.sendReport('ABORTED',targetChannel,"Build Discarded: ${env.JOB_NAME} - ${env.BUILD_NUMBER}")
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        stage('🚀 Deploy to prod') {
            when {
                branch 'master'
            }
            steps {
                script {
                    echo 'Deploy to PROD'
                    slack.sendReport('DEPLOY',targetChannel,"Build ${currentBuild.displayName} successfully deployed to prod")
                }
            }
        }

        stage('Tag release') {
            when {
                anyOf {
                    expression {
                        return params.tag
                    }
                    branch 'master'
                }
            }
            steps {
                script {
                    echo 'Tag release'
                }
            }
        }
    }

  // ========================================================================== //
  // Post
  // ========================================================================== //
  // https://www.jenkins.io/doc/book/pipeline/syntax/#post

    post {
        // Always runs. And it runs before any of the other post conditions.
        always {
            // recordIssues(
            //     enabledForFailure: true, aggregatingResults: true,
            //     tools: [
            //         java(),
            //     ]
            // )

            // echo '📊 Publishing HTML report'
            // publishHTML(target:[
            //     allowMissing: true,
            //     alwaysLinkToLastBuild: true,
            //     keepAll: true,
            //     reportDir: "${WORKSPACE}",
            //     reportFiles: 'report.html',
            //     reportName: 'CI-Build-HTML-Report',
            //     reportTitles: 'CI-Build-HTML-Report'
            // ])

            // recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), javaDoc()]
            // recordIssues enabledForFailure: true, tool: checkStyle()
            // recordIssues enabledForFailure: true, tool: cpd(pattern: '**/target/cpd.xml')
            // recordIssues enabledForFailure: true, tool: hadolint()
            // recordIssues enabledForFailure: true, tool: junit()
            // recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
            // recordIssues enabledForFailure: true, tool: spotBugs()
            // recordIssues enabledForFailure: true, tool: yamlLint()

            echo 'One way or another, I have finished'
        }
        aborted {
            script {
                def msg = 'Build aborted 😵'
                echo msg
                slack.sendReport('ABORTED',targetChannel, msg)
            }
        }
        success {
            script {
                def msg = 'I succeeded! 😊'
                echo msg
                slack.sendReport('SUCCESS',targetChannel, msg)
             }
        }
        unstable {
            script {
                def msg = 'I am unstable 😈'
                echo msg
                slack.sendReport('UNSTABLE',targetChannel, msg)
            }
        }
        failure {
            script {
                def msg = 'I failed 😱'
                echo msg
                slack.sendReport('FAILURE',targetChannel,msg)
            }
        }
        changed {
            // only runs if status changed from last run
            echo 'Things were different before...'
        }
        cleanup {
            // runs after all other post conditions
            echo 'Cleanup workspace'
            deleteDir() /* clean up our workspace */
        }
    }
}

//  vim:filetype=groovy:syntax=groovy
