import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

// Work in progress to use the new Slack Blocks API
// Do not use yet in any production pipeline

def call (String buildStatus = 'STARTED', String slackChannel = '#jenkins', String extraInfoRaw = '') {

    // buildStatus of null means successful
    buildStatus = buildStatus ?: 'SUCCESSFUL'

    // channel null sends to default channel
    def channel = slackChannel ?: '#jenkins'

    // Random Icon
    def icons = [':unicorn_face:', ':beer:', ':bee:', ':man_dancing:', ':rabbit:',
        ':ghost:', ':dancer:', ':scream_cat:']
    def randomIndex = (new Random()).nextInt(icons.size())

    // Default values
    def color = '#DCDCDC'
    def icon = ':rocket:'

    switch (buildStatus) {
        case 'STARTED':
            color = '#FFFF00'
            icon = "${icons[randomIndex]}"
            break
        case 'FAILURE':
            color = 'danger'
            icon = ':shit:'
            break
        case ['SUCCESS', 'SUCCESSFUL']:
            color = 'good'
            icon = "${icons[randomIndex]}"
            break
        case 'UNSTABLE':
            color = 'warning'
            icon = ':fire:'
            break
        case 'ABORTED':
            color = '#878686'
            icon = ':x:'
            break
        default:
            color = '#DCDCDC'
            icon = ':information_source:'
            break
    }

    // Commit Info
    def branchName = "${env.BRANCH_NAME}"

    //def commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
    def commitMessage = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()

    // def authorName = sh(returnStdout: true, script: "git --no-pager show -s --format='%an'").trim()
    // def authorEmail = sh(returnStdout: true, script: "git --no-pager show -s --format='%ae'").trim()

    def userIds = slackUserIdsFromCommitters()
    def userIdsString = userIds.collect { "<@$it>" }.join(' ')
    def previousBuildField = currentBuild.previousSuccessfulBuild == null ?
        'None' :
        "<${currentBuild.previousSuccessfulBuild.absoluteUrl}|${currentBuild.previousSuccessfulBuild.displayName}>"


    // format extraInfo as a code block
    def extraInfo = """
        ```${extraInfoRaw}```
    """

    def fullMessage = icon + ' ' + extraInfo

    // Slack message block

    textNotification = "${buildStatus}: Job ${currentBuild.projectName} Build: ${currentBuild.number} (<${env.RUN_DISPLAY_URL}|open>) (<${env.RUN_CHANGES_DISPLAY_URL}|changes>)"

    blocks = [
        [
            'type': 'header',
            'text': [
                'type': 'plain_text',
                'text': "${currentBuild.fullDisplayName}",
            'emoji': true
            ]
        ],
        [
            'type': 'section',
                'text': [
                'type': 'mrkdwn',
                'text': "${buildStatus}: Job ${currentBuild.projectName} Build: ${currentBuild.number} (<${env.RUN_DISPLAY_URL}|open>) (<${env.RUN_CHANGES_DISPLAY_URL}|changes>)"
            ]
        ],
        [
            'type': 'section',
                'fields': [
                    [
                        'type': 'mrkdwn',
                        'text': "*Previous Successful build:*\n${previousBuildField}"
                    ]
                ]
        ],
        [
            'type': 'section',
                'fields': [
                    [
                        'type': 'mrkdwn',
                        'text': "*Branch:*\n${branchName}"
                    ],
                    [
                        'type': 'mrkdwn',
                        'text': "*Authors:*\n${userIdsString}"
                    ],
                ]
        ],
        [
            'type': 'section',
            'fields': [
                [
                    'type': 'mrkdwn',
                    'text': "*Commit Message:*\n${commitMessage}"
                ],
            ]
        ],
        [
            'type': 'divider'
        ],
        [
            'type': 'section',
        'fields': [
            [
                'type': 'mrkdwn',
        'text': "*Information:*\n${extraInfo}"
            ]
        ]
        ],
        [
            'type': 'actions',
            'elements': [
                [
                    'type': 'button',
                    'text': [
                        'type': 'plain_text',
                        'text': 'Click here to see Live Output'
                    ],
                    'url': "${env.BUILD_URL}console"
                ]
            ]
        ]
    ]


    // // Find the most recent merge commit and print its parents. Merge commits have 2 parent commits: the base and the head.
    // def merge_hashes = sh(returnStdout: true, script:"git log --first-parent ${branchName} --merges -n 1 --format='%P'").trim()
    // def (base_commit, latest_commit) = merge_hashes.tokenize(' ')
    // // List the author email and subject of all commits between the base (excluding) and the head (inclusive) of the latest merge commit
    // def commit_list = sh(returnStdout: true, script:"git log ${latest_commit} --not ${base_commit} --format='%ae %H %s' --no-merges").trim()
    // def remote_url = sh(returnStdout: true, script:"git config --get remote.origin.url").trim()
    // remote_url = remote_url.replace('.git', '')

    // // Turn the list of commits into slack block kit sections, tagging the authors
    // def commits = commit_list.split("\n")

    // def slack_block_limit = 50;
    // def slack_blocks_per_commit = 3;
    // def total_commits = commits.size();
    // def total_commits_shown = total_commits;
    // def total_commits_truncated = 0;
    // // If the slack message would go over "slack_block_limit", cut it down
    // if (blocks.size() + (total_commits * slack_blocks_per_commit) > slack_block_limit) {
    //     total_commits_shown = (int) Math.floor((slack_block_limit - blocks.size() - 1) / slack_blocks_per_commit);
    //     total_commits_truncated = total_commits - total_commits_shown;
    //     commits = Arrays.copyOfRange(commits, 0, total_commits_shown);
    // }

    // commits.each { commit ->
    //     def row = commit.split(' ', 3)
    //     def author_email = row[0]
    //     def hash = row[1]
    //     def subject = row[2]
    //     def slack_user_id = slackUserIdFromEmail(author_email)

    //     blocks.push([
    //         "type": "section",
    //         "text": ["type": "mrkdwn","text": "*<${remote_url}/commit/${hash}|${subject}>*"]
    //     ])
    //     blocks.push([
    //         "type": "context",
    //         "elements": [["type": "mrkdwn","text": "By <@${slack_user_id}> <${author_email}>"]]
    //     ])
    //     blocks.push(["type": "divider"])
    // }

    // if (total_commits_truncated > 0) {
    //     blocks.push([
    //         "type": "section",
    //         "text": [
    //             "type": "mrkdwn",
    //             "text": "*This deploy includes ${total_commits_truncated} additional commits.* Try squashing your commits when possible."
    //         ]
    //     ])
    // }

    // Send Slack message
    //slackSend (color: color, message: title, attachments: attachments.toString(), channel: channel, iconEmoji: icon)
    //Blocks cannot use Color, needs to be passed as attachment
    slackSend(message: textNotification, blocks: blocks, channel: channel, iconEmoji: icon)
}
