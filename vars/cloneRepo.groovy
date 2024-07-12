import org.xaas.utils.GitRepoUtils

/**
 * cloneRepo step
 *
 * Jenkins shared library wrapper for cloning a Git repository using GitRepoUtils.
 *
 * Usage in Jenkinsfile:
 *   cloneRepo(
 *       url: "https://github.com/org/repo.git",
 *       branch: "main",
 *       credsId: "github-credentials-id",
 *       targetDir: "workspace/repo"
 *   )
 *
 * @param url       (Required) Git repository URL
 * @param branch    (Required) Branch to clone
 * @param credsId   (Required) Jenkins credentials ID
 * @param targetDir (Optional) Directory to clone into (default: ".")
 */
def call(Map args = [:]) {
    if (!args.url?.trim()) {
        error("[ERROR] 'url' is required")
    }
    if (!args.branch?.trim()) {
        error "[ERROR] 'branch' is required"
    }
    if (!args.credsId?.trim()) {
        error "[ERROR] 'credsId' is required"
    }

    def targetDir = args.targetDir ?: "."

    def gitRepoUtils = new GitRepoUtils(this)
    gitRepoUtils.cloneRepo(args.url, args.branch, args.credsId, targetDir)
}
