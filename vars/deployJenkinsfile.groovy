import org.xaas.utils.GitRepoUtils

/**
 * deployJenkinsfile.groovy - Shared library step for deploying a Jenkinsfile into a client repository.
 *
 * This step:
 * 1. Clones the target client repository from GitHub.
 * 2. Copies a provided Jenkinsfile into the repository under the client’s directory.
 * 3. Commits and pushes the changes back to the remote repository.
 *
 * Arguments:
 *   @param repoUrl      (String) URL of the client Git repository.
 *   @param targetBranch (String) Branch where the Jenkinsfile should be committed (e.g., "main").
 *   @param clientName   (String) Unique name of the client; used as the directory path.
 *   @param sourceFile   (String) Path to the source Jenkinsfile template to copy.
 *
 * Usage (in Jenkinsfile):
 *   deployJenkinsfile(
 *       repoUrl: "https://github.com/myorg/client-repo.git",
 *       targetBranch: "main",
 *       clientName: "AcmeCorp",
 *       sourceFile: "./templates/Jenkinsfile"
 *   )
 */
def call(Map args) {
    def repoUrl      = args.repoUrl ?: error("[ERROR] repoUrl is required")
    def targetBranch = args.targetBranch ?: error("[ERROR] targetBranch is required")
    def clientName   = args.clientName ?: error("[ERROR] clientName is required")
    def sourceFile   = args.sourceFile ?: error("[ERROR] sourceFile is required")

    def gitRepoUtils = new GitRepoUtils(this)
    def workDir      = "jenkins-xaas-client-repo"

    echo "[INFO] Cloning repository: ${repoUrl} (branch: ${targetBranch})"

    /*
    * Clone jenkins-xaas-client-piplines repo for depploying newly generated Jenkinsfile
    * IMPORTANT:
    * - The repo URL and credentials must match in style:
    *   → If using SSH URL (`git@github.com:...`), use SSH credentials.
    *   → If using HTTPS URL (`https://github.com/...`), use HTTPS token.
    * - Mixing styles (SSH URL with HTTPS creds or vice versa) will fail.
    */
    cloneRepo(
        url: repoUrl,
        branch: targetBranch,
        credsId: "${env.GITHUB_HTTPS_TOKEN}",
        targetDir: workDir
    )

    echo "[INFO] Copying Jenkinsfile into ${workDir}/${clientName}/Jenkinsfile"
    sh(
        label: "Copying Jenkinsfile into ${workDir}/${clientName}",
        script: """
            cd "${workDir}"
            mkdir -p "${clientName}"
            cp "${sourceFile}" "${clientName}/Jenkinsfile"
        """
    )

    echo "[INFO] Committing and pushing Jenkinsfile for client: ${clientName}"
    gitRepoUtils.commitAndPush(
        repoUrl,
        "Add/Update Jenkinsfile for client ${clientName}",
        targetBranch,
        workDir,
        "${env.GITHUB_HTTPS_TOKEN}"
    )

    echo "[SUCCESS] Jenkinsfile deployed successfully for client: ${clientName}"
}
