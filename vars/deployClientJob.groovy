import org.xaas.utils.JenkinsApiUtils

/**
 * deployClientJob.groovy - Shared library step for creating Client Jenkins pipeline job and
 *                          and triggering first build
 *
 * This step:
 * 1. Reads an XML job template from resources.
 * 2. Replaces placeholders with runtime values (repo URL, client name, GitHub token).
 * 3. Creates the Jenkins job via REST API.
 * 4. Immediately triggers the newly created job.
 *
 * Usage (in Jenkinsfile):
 *   deployClientJob(
 *       clientName: "my-client",
 *       repoUrl: "https://github.com/org/repo.git"
 *   )
 */
def call(Map args) {
    // Validate required arguments
    def clientName = args.clientName ?: error("[ERROR] clientName is required")
    def repoUrl    = args.repoUrl ?: error("[ERROR] repoUrl is required")

    
    // --- Initialize Jenkins API utility (allow test injection) ---
    def jenkinsApiUtils = this.respondsTo("newJenkinsApiUtils") ?
                          this.newJenkinsApiUtils() :
                          new JenkinsApiUtils(this)

    // Job name convention: <clientName>-pipeline
    def jobName = "${clientName}-pipeline"

    // Path to XML job template in the shared library resources
    def templatePath = "job-deployer/job-templates/job-template.xml"

    echo "[INFO] Loading XML job template for ${jobName} from ${templatePath}..."

    // Load XML template and replace placeholders
    def xmlContent = libraryResource(templatePath)
        .replace('${REPO_URL}', repoUrl)
        .replace('${CLIENT_NAME}', clientName)
        .replace('${GITHUB_HTTPS_TOKEN}', "${env.GITHUB_HTTPS_TOKEN}")

    echo "[INFO] Creating Jenkins job '${jobName}' via REST API..."
    jenkinsApiUtils.createJob(jobName, xmlContent)

    echo "[INFO] Triggering Jenkins job '${jobName}'..."
    jenkinsApiUtils.triggerJob(jobName)

    echo "[INFO] Job '${jobName}' created and triggered successfully."
}
