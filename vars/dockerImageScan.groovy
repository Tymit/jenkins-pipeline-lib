#!groovy
/**
 * Run a vulnerabilty scan on a Docker Image using Trivy
 *
 * Parameters:
 *
 *     `image` (required): the image to be scanned.
 *
 *  Usage (e.g. in a pipeline script):
 *
 *     dockerImageScan('quay.io/my-org/my-image:latest')
 *
 */

def call(String dockerImage) {

  echo '🔎 Running Docker image Check'
  sh(
      label: 'Docker Image Check',
      script: """#!/bin/bash
        set -ex

        docker volume create --name trivy-cache

        docker run \
            --pull=always \
            -v /var/run/docker.sock:/var/run/docker.sock \
            -v trivy-cache:/root/.cache/trivy \
            -v ${env.WORKSPACE}:/src \
            aquasec/trivy:latest \
            image --output /src/trivy-report.txt ${dockerImage}
      """
  )
}
