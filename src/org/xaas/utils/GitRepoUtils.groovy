package org.xaas.utils

/**
 * GitRepoUtils - Utility class for performing Git operations in Jenkins pipelines.
 * 
 * This class provides methods to:
 * 1. Clone a Git repository into a specified directory.
 * 2. Commit changes and push to a remote repository.
 *
 * It uses Jenkins pipeline `script` context for executing `git` and `sh` steps.
 * Logging is added via `script.echo` for better visibility in Jenkins console output.
 *
 * Usage:
 *   def gitRepoUtils = new GitRepoUtils(script)
 *   gitRepoUtils.cloneRepo("https://github.com/...", "main", "credsId", "targetDir")
 *   gitRepoUtils.commitAndPush("https://github.com/...", "Commit message", "main", "targetDir", "credsId")
 */
class GitRepoUtils implements Serializable {

    def script

    /**
     * Constructor to initialize the utility with the pipeline script context
     * @param script - Jenkins pipeline script context
     */
    GitRepoUtils(script) {
        this.script = script
    }

    /**
     * Clone a Git repository into the specified target directory
     * @param url - Git repository URL
     * @param branch - Branch to clone
     * @param credsId - Jenkins credentials ID
     * @param targetDir - Directory to clone into (default: current directory)
     */
    def cloneRepo(String url, String branch, String credsId, String targetDir = ".") {
        if (!url?.trim()) {
            throw new IllegalArgumentException("[ERROR] 'url' is required")
        }
        if (!branch?.trim()) {
            throw new IllegalArgumentException("[ERROR] 'branch' is required")
        }
        if (!credsId?.trim()) {
            throw new IllegalArgumentException("[ERROR] 'credsId' is required")
        }
        script.echo("Cloning repository ${url} (branch: ${branch}) into directory: ${targetDir}")
        script.dir(targetDir) {
            script.git(
                credentialsId: credsId,
                url: url,
                branch: branch
            )
        }
        script.echo("Repository cloned successfully.")
    }

    /**
     * Commit changes and push to the remote repository
     * @param url - Git repository URL
     * @param message - Commit message
     * @param branch - Branch to push (default: main)
     * @param targetDir - Directory of the repository (default: current directory)
     * @param credsId - Jenkins credentials ID
     */
    def commitAndPush(String url, String message, String branch = "main", String targetDir = ".", String credsId) {
        script.echo("Committing and pushing changes to ${url} (branch: ${branch}) in directory: ${targetDir}")
        def repoHost = url.replace("https://", "")

        script.withCredentials([
            script.usernamePassword(
                credentialsId: credsId,
                usernameVariable: 'GIT_USER',
                passwordVariable: 'GIT_PASS'
        )]) {
            script.sh(
                label: "Push changes to repo",
                script: """
                    cd "${targetDir}"
                    git config user.name "\$GIT_USER"
                    git config user.email "\$GIT_USER@noreply.github.com"
                    git remote set-url origin "https://\$GIT_USER:\$GIT_PASS@${repoHost}"
                    git add .
                    git commit -m "${message}" || echo "No changes to commit"
                    git push origin ${branch}
                """
            )
        }

        script.echo("Push completed successfully.")
    }
}
