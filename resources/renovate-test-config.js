module.exports = {
  binarySource: "docker",
  dryRun: false,
  printConfig: false,
  platform: "bitbucket",
  timezone: "Europe/Madrid",
  username: "tymitjenkins",
  password: "${BITBUCKET_PASSWORD}",
  labels: ["renovate"],
  npm: {
    postUpdateOptions: ["npmDedupe", "yarnDedupeFewer"],
  },
  repositories: [
    "gangsplit/ami-jenkins-agent",
    "gangsplit/ami-jenkins-android",
    "gangsplit/api-gateway",
    "gangsplit/ecs-fluentbit-sidecar",
    "gangsplit/es-curator",
    "gangsplit/infrastructure",
    "gangsplit/java-builder",
    "gangsplit/java-runner",
    "gangsplit/jenkins-pipeline-lib",
    "gangsplit/metabase",
    "gangsplit/node-builder",
    "gangsplit/platform-cli",
    "gangsplit/platform-monitoring",

  ],
  gitUrl: "ssh",
  onboarding: false,
  onboardingConfig: {
    extends: [
      "config:base",
      ":automergeDisabled",
      ":disableDependencyDashboard",
      ":rebaseStalePrs",
      "docker:enableMajor",
      "group:recommended",
      "group:monorepos",
    ],
  },
  persistRepoData: true,
  requireConfig: false,
  baseBranches: ["develop", "development"],
  rebaseWhen: "behind-base-branch",
  // schedule: [
  //   "after 10pm every weekday",
  //   "before 5am every weekday",
  //   "every weekend",
  // ],
  prCreation: "not-pending",
  bbUseDefaultReviewers: true,
  reviewersFromCodeOwners: true,
  regexManagers: [
    {
      fileMatch: ["(^|/)Dockerfile$"],
      matchStrings: [
        '#\\s*renovate:\\s*datasource=(?<datasource>.*?) depName=(?<depName>.*?)( versioning=(?<versioning>.*?))?( extractVersion=(?<extractVersion>.*?))?\\s(ENV|ARG) .*?_VERSION="?(?<currentValue>.*?)"?\\s',
      ],
      versioningTemplate:
        "{{#if versioning}}{{versioning}}{{else}}semver{{/if}}",
    },
    {
      fileMatch: ["\\.tf$"],
      matchStrings: [
        '\\s*container_image\\s*=\\s*"(?<depName>.*?):(?<currentValue>.*?)"?\\s',
      ],
      datasourceTemplate: "docker"
    },
  ],
  "packageRules": [
    {
      "matchPackageNames": ["postgres", "rabbitmq"],
      "enabled": false
    },
    {
      description: "Allow updates after 3 days (exclude renovate)",
      excludePackageNames: ["renovate/renovate"],
      separateMultipleMajor: true,
      separateMinorPatch: true,
      stabilityDays: 0,
    },
    {
      "description": "Schedule digest updates daily",
      "matchUpdateTypes": ["digest"],
      "extends": ["schedule:daily"]
    },
  ],
  // "packageRules": [
  //   {
  //     "description": "Trigger breaking release for major updates",
  //     "matchPackageNames": [
  //       "docker",
  //       "renovate/renovate"
  //     ],
  //     "matchUpdateTypes": ["major"],
  //     "semanticCommitType": "feat",
  //     "commitBody": "BREAKING CHANGE: Major update",
  //     "automergeType": "pr"
  //   },
  //   {
  //     "description": "Trigger feature release for minor updates",
  //     "matchPackageNames": [
  //       "docker",
  //       "renovate/renovate"
  //     ],
  //     "matchUpdateTypes": ["minor"],
  //     "semanticCommitType": "feat",
  //     "automergeType": "pr"
  //   },
  //   {
  //     "description": "Trigger fix release for patch updates",
  //     "matchPackageNames": [
  //       "docker",
  //       "renovate/renovate"
  //     ],
  //     "matchUpdateTypes": ["patch", "digest"],
  //     "semanticCommitType": "fix",
  //     "automergeType": "pr"
  //   },
  //   {
  //     "matchPackageNames": ["renovate/node"],
  //     "versioning": "node"
  //   }
  // ],
  //ignoreDeps: ["*"],
  updateInteralDeps: true,

};
