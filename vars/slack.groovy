import net.sf.json.JSONArray
import net.sf.json.JSONObject

def sendReport (String buildStatus = 'STARTED', String channel = '#jenkins', String extraInfoRaw = '') {

  // buildStatus of null means successful
  buildStatus = buildStatus ?: 'SUCCESSFUL'
  channel = channel ?: '#jenkins'

  def icons = [":unicorn_face:", ":beer:", ":bee:", ":man_dancing:", ":rabbit:",
    ":ghost:", ":dancer:", ":scream_cat:"]
  def randomIndex = (new Random()).nextInt(icons.size())

  // Default values
  def subject = "${buildStatus}: Job ${currentBuild.projectName} Build: ${currentBuild.number} (<${env.RUN_DISPLAY_URL}|open>) (<${env.RUN_CHANGES_DISPLAY_URL}|changes>)"
  def title = "${currentBuild.fullDisplayName}"
  def title_link = "${env.BUILD_URL}"
  def branchName = "${env.BRANCH_NAME}"

  def commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
  def authorName = sh(returnStdout: true, script: "git --no-pager show -s --format='%an'").trim()
  def authorEmail = sh(returnStdout: true, script: "git --no-pager show -s --format='%ae'").trim()

  def message = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()

  // Override default values based on build status
  def color = '#DCDCDC'
  def mention = ''
  def icon = ':rocket:'

  switch (buildStatus) {
    case 'STARTED':
        color = '#FFFF00'
        icon = "${icons[randomIndex]}"
        break
    case 'FAILURE':
        color = 'danger'
        icon = ':shit:'
        mention = '@' + authorName.toString()
        break
    case 'SUCCESS':
        color = 'good'
        icon = "${icons[randomIndex]}"
        mention = '<!here>'
        break
    case 'UNSTABLE':
        color = 'warning'
        icon = ':fire:'
        mention = '@' + authorName.toString()
        break
    case 'ABORTED':
        color = '#878686'
        icon = ':x:'
        break
    case 'INFO':
        icon = ":information_source:"
        break
    default:
        icon = ':face_palm:'
        break
    }


  // format extraInfo as a code block
  def extraInfo = """
    ```${extraInfoRaw}```
  """

  def fullMessage = icon + ' ' + mention + ' ' + extraInfo

  // JSONObject for branch
  JSONObject branch = new JSONObject();
  branch.put('title', 'Branch');
  branch.put('value', branchName.toString());
  branch.put('short', true);

  // JSONObject for author
  JSONObject commitAuthor = new JSONObject();
  commitAuthor.put('title', 'Author');
  commitAuthor.put('value', authorName.toString() + ' <' + authorEmail.toString() +'>');
  commitAuthor.put('short', true);

  // JSONObject for branch
  JSONObject commitMessage = new JSONObject();
  commitMessage.put('title', 'Commit Message');
  commitMessage.put('value', message.toString());
  commitMessage.put('short', false);

  // JSONObject for Information display
  JSONObject infoMessage = new JSONObject();
  infoMessage.put('title', 'Information')
  infoMessage.put('value', fullMessage.toString())
  infoMessage.put('short', false)

  // JSONObject for attachment
  JSONObject attachment = new JSONObject();
  attachment.put('author',"jenkins");
  attachment.put('author_link',"https://jenkins.dev.tymit.io");
  attachment.put('title', title.toString());
  attachment.put('title_link',title_link.toString());
  attachment.put('text', subject.toString());
  attachment.put('fallback', "fallback message");
  attachment.put('color',color);
  attachment.put('mrkdwn_in', ["fields"])
  attachment.put('fields', [branch, commitAuthor, commitMessage, infoMessage]);

  JSONArray attachments = new JSONArray();
  attachments.add(attachment);

  // Send notifications
  slackSend (color: color, message: title, attachments: attachments.toString(), channel: channel, iconEmoji: icon)

}
