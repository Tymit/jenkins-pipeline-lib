#!/usr/bin/make -f

## Makefile default opts
# MAKEFLAGS += --warn-undefined-variables
# Use bash not sh
SHELL := bash
.SHELLFLAGS := -e -o pipefail -c
.DEFAULT_GOAL := help
.DELETE_ON_ERROR:
.SUFFIXES:

INTERACTIVE := $(shell [ -t 0 ] && echo 1)
ifdef INTERACTIVE
    # For interactive invocations, interleave everything, since otherwise
    # shell targets will hang forever not echoing
    # output.
    MAKEFLAGS += -j 4 --no-builtin-rules
else
    MAKEFLAGS += -j 4 --no-builtin-rules --output-sync=line
endif

WORKDIR=$(dir $(realpath $(firstword $(MAKEFILE_LIST))))

# Populate version variables with commit information
GITREPO = $(shell git rev-parse --is-inside-work-tree)
ifeq ($(GITREPO),true)
GITBRANCH := $(shell git rev-parse --abbrev-ref HEAD | sed 's/\//_/g')
GITCOMMIT := $(shell git rev-parse --short HEAD)
GITUNTRACKEDCHANGES := $(shell git status --porcelain --untracked-files)
LATEST := latest
endif
ifneq ($(GITUNTRACKEDCHANGES),)
GITCOMMIT := $(GITCOMMIT)-dirty
LATEST := latest-dirty
endif

BUILD_DATE := $(shell date -u +"%Y-%m-%dT%H:%M:%SZ")

## Makefile content
# Force to use buildkit for all images and for docker-compose to invoke
# Docker via CLI (otherwise buildkit isn't used for those images)
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
export COMPOSE_INTERACTIVE_NO_CLI=1
# BuildKit uses fancy output by default, making CI logs for multiple jobs
# unreadable.
export BUILDKIT_PROGRESS=plain

# ECR Repository variables
ACCOUNT_ID = 722057856351
ECR_REGION = eu-west-1
ECR := $(ACCOUNT_ID).dkr.ecr.$(ECR_REGION).amazonaws.com
CONTAINER_NAME := $(ECR)/platform/platform-cli
CONTAINER_VERSION := develop-latest

DOCKER_IMAGE := $(CONTAINER_NAME):$(CONTAINER_VERSION)
DOCKER_RUN_OPTS = --rm --interactive --tty
DOCKER_RUN_OPTS += -e GIT_DISCOVERY_ACROSS_FILESYSTEM=true
DOCKER_RUN_OPTS += --user $$(id -u):$$(id -g)
DOCKER_RUN_OPTS += -v $(WORKDIR):/workspace:Z
DOCKER_RUN_EXTRA_OPTS = -e ENVIRONMENT=${ENVIRONMENT}
DOCKER_RUN_ENV_AWS = -e AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
DOCKER_RUN_ENV_AWS += -e AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
DOCKER_RUN_ENV_AWS += -e AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN}
DOCKER_RUN_ENV_AWS += -e AWS_SECURITY_TOKEN=${AWS_SECURITY_TOKEN}
DOCKER_RUN_ENV_AWS += -e AWS_DEFAULT_REGION=${AWS_REGION}
DOCKER_RUN_ENV_AWS += -e AWS_REGION=${AWS_REGION}

RENOVATE_BASE_DIR=$(WORKDIR)/tmp/renovate/
DOCKER_RUN_ENV_RENOVATE = -e RENOVATE_AUTODISCOVER=false
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_BASE_DIR=$(RENOVATE_BASE_DIR)
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_DOCKER_USER=$$(id -u):$$(getent group docker | cut -d ':' -f 3)
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_LOG_FILE=$(RENOVATE_BASE_DIR)/renovate-log.ndjson
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_LOG_FILE_LEVEL=info
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_ONBOARDING=false
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_OPTIMIZE_FOR_DISABLED=true
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_PLATFORM=bitbucket
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_REPOSITORY_CACHE=true
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_REQUIRE_CONFIG=false
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_TOKEN=${RENOVATE_TOKEN}
DOCKER_RUN_ENV_RENOVATE += -e GITHUB_COM_TOKEN=${GITHUB_API_TOKEN}
DOCKER_RUN_ENV_RENOVATE += -e LOG_LEVEL=info
DOCKER_RUN_ENV_RENOVATE += -e RENOVATE_CACHE_NPM_MINUTES=180


.PHONY: ALL

help:
	@echo "usage: make [target] ..."
	@echo ""
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "(prefix a target with '_' to run in a container, ie: 'make _plan')"
	@echo "(suffix a target with '-docker' to run in a container, ie: 'make plan=docker')"

set-env:
	@if [ -z $(ENVIRONMENT) ]; then \
		echo "ENVIRONMENT was not set"; exit 10;\
	fi

dockerlogin: ## Docker login into ECR registry
	@aws ecr get-login-password --region $(ECR_REGION) | docker login --username AWS --password-stdin $(ECR) > /dev/null 2>&1

_%: dockerlogin
	@docker run $(DOCKER_RUN_OPTS) $(DOCKER_RUN_EXTRA_OPTS) $(DOCKER_IMAGE) \
		"make $*"

%-docker: dockerlogin
	@docker run $(DOCKER_RUN_OPTS) $(DOCKER_RUN_EXTRA_OPTS) $(DOCKER_IMAGE) \
		"make $*"

shell: dockerlogin ## Run shell in container
	@echo -e "[+] Running shell in container $(DOCKER_IMAGE)"
	@docker run --tty $(DOCKER_RUN_OPTS) $(DOCKER_RUN_EXTRA_OPTS) $(DOCKER_IMAGE)

lint: ## Lint all files
	@echo -e "[+] Linting all files"
	docker run $(DOCKER_RUN_OPTS) \
		-e SHOW_ELAPSED_TIME=true \
		-v $(WORKDIR):/tmp/lint:Z \
		megalinter/megalinter:latest

sast: ## Run Static Analysis Security Test
	@echo -e "[+] Running Static Analysis Security Test"
	docker run \
		-e SEMGREP_RULES="p/owasp-top-ten p/security-audit p/ci p/auto" \
		-v $(WORKDIR):/src \
		returntocorp/semgrep:latest \
		semgrep scan --metrics=off --error

sast-report: ## Run Static Analysis Security Test and write a report
	@echo -e "[+] Running Static Analysis Security Test report"
	docker run \
		-e SEMGREP_RULES="p/owasp-top-ten p/security-audit p/ci p/auto" \
		-v $(WORKDIR):/src \
		returntocorp/semgrep:latest \
		semgrep scan --metrics=off --no-error --sarif -o sast-report.sarif -q

sast-mobile: ## Run Static Analysis Security Test for mobile
	@echo -e "[+] Running Static Analysis Security Test"
	docker run $(DOCKER_RUN_OPTS) \
		opensecurity/mobsfscan:0.1.0 /workspace

sast-mobile-html: ## Run Static Analysis Security Test for mobile and write a HTML report
	@echo -e "[+] Running Static Analysis Security Test report"
	docker run $(DOCKER_RUN_OPTS) \
		opensecurity/mobsfscan:0.1.0 --html -o /src/sast-report.html /workspace

dast: ## Run Dynamic Analysis Security Test
	@echo -e "[+] Dynamic Analysis Security Test"
	docker run $(DOCKER_RUN_OPTS) \
		-p 8080:8080 \
		-p 9080:9080 \
		owasp/zap2docker-stable \
		zap-webswing.sh

depcheck: ## Run Dependency Check
	@echo -e "[+] Running Dependency Check"
	@mkdir -p /tmp/depcheck/data/cache
	docker volume create --name dependency-check-cache
	docker run $(DOCKER_RUN_OPTS) \
		-e user=$$(id -u) \
		-v $(WORKDIR):/src:z \
		-v dependency-check-cache:/usr/share/dependency-check/data \
		owasp/dependency-check:latest \
		--scan /src --format "ALL"

# containercheck: ## Scan vulnerabilities in container
#     @echo -e "[+] Running Container Check"
#     docker volume create --name trivy-cache
#     docker run \
#         -v /var/run/docker.sock:/var/run/docker.sock \
#         -v trivy-cache:/.cache/trivy \
#         -v $(WORKDIR):/src:z \
#         aquasec/trivy:latest \
#         --cache-dir /.cache/trivy image --output /src/trivy-report.txt $(DOCKER_IMAGE)
#
containercheck: ## Scan vulnerabilities in container
	@echo -e "[+] Running Container Check"
	docker volume create --name trivy-cache
	docker run \
		-v /var/run/docker.sock:/var/run/docker.sock \
		-v trivy-cache:/root/.cache/trivy \
		-v $(WORKDIR):/src:z \
		aquasec/trivy:latest \
		image --output /src/trivy-report.txt $(DOCKER_IMAGE)

renovate: ## Run Renovate bot
	@echo -e "[+] Running renovate"
	@mkdir -p $(WORKDIR)/tmp/renovate && chown $$(id -u):$$(getent group docker | cut -d ':' -f 3) $(WORKDIR)/tmp/renovate
	@chmod ug+rw $(WORKDIR)/tmp/renovate
	@docker run $(DOCKER_RUN_OPTS) $(DOCKER_RUN_ENV_RENOVATE) \
		--privileged \
		-v /var/run/docker.sock:/var/run/docker.sock \
		-v "$(RENOVATE_BASE_DIR):$(RENOVATE_BASE_DIR):Z" \
		-v "$(WORKDIR)/resources/keys/renovate/:/home/ubuntu/.ssh" \
		-v "$(WORKDIR)/resources/renovate-config.js:/usr/src/app/config.js" \
		renovate/renovate:slim

renovate-test-config: ## Run Renovate bot to test configs
	@echo -e "[+] Running renovate test config"
	@mkdir -p $(WORKDIR)/tmp/renovate && chown $$(id -u):$$(getent group docker | cut -d ':' -f 3) $(WORKDIR)/tmp/renovate
	@chmod ug+rw $(WORKDIR)/tmp/renovate
	@docker run $(DOCKER_RUN_OPTS) $(DOCKER_RUN_ENV_RENOVATE) \
		--privileged \
		-v /var/run/docker.sock:/var/run/docker.sock \
		-v "$(RENOVATE_BASE_DIR):$(RENOVATE_BASE_DIR):Z" \
		-v "$(WORKDIR)/resources/keys/renovate/:/home/ubuntu/.ssh" \
		-v "$(WORKDIR)/resources/renovate-test-config.js:/usr/src/app/config.js" \
		renovate/renovate:slim
