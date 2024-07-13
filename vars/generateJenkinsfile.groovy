/**
 * generateJenkinsfile.groovy - Shared library step to generate a Jenkinsfile using pipeline-gen.
 *
 * This step:
 * 1. Resolves config file and output file paths.
 * 2. Builds CLI arguments for pipeline-gen based on provided parameters.
 * 3. Runs pipeline-gen to generate the Jenkinsfile inside the Jenkinsfile generator container.
 *
 * Arguments (Map args):
 *   @param configFile     (String) Path to config file (relative or absolute).
 *   @param outputFile     (String) Output Jenkinsfile path. Defaults to "output/Jenkinsfile".
 *   @param templateName   (String) Template name to use (required).
 *   @param branchName     (String) Optional branch name override.
 *   @param buildTool      (String) Optional build tool override.
 *   @param buildSteps     (String) Optional build steps override.
 *   @param repoUrl        (String) Optional Git repository URL.
 *   @param previewMode    (boolean) Run generator in preview mode.
 *   @param validateOnly   (boolean) Run generator with validate-only flag.
 *   @param strictMode     (boolean) Run generator with strict flag.
 *
 * Usage (in Jenkinsfile):
 *   generateJenkinsfile(
 *       configFile: "configs/app-config.yaml",
 *       outputFile: "output/Jenkinsfile",
 *       templateName: "java-maven",
 *       branchName: "main",
 *       buildTool: "maven",
 *       buildSteps: "clean install",
 *       repoUrl: "https://github.com/org/repo.git",
 *       previewMode: true,
 *       validateOnly: false,
 *       strictMode: true
 *   )
 */
def call(Map args = [:]) {
    def workDir      = env.JENKINSFILE_GENERATOR_DIR
    
    if (!args.configFile?.trim()) {
        error("[ERROR] configFile is required and cannot be null or empty")
    }
    if (!args.outputFile?.trim()) {
        error("[ERROR] outputFile is required and cannot be null or empty")
    }
    if (!args.templateName?.trim()) {
        error("[ERROR] templateName is required and cannot be null or empty")
    }

    def configFile = (args.configFile?.startsWith("configs/")) 
                     ? "${workDir}/${args.configFile}" 
                     : args.configFile
    def outputFile = (args.outputFile == "output/Jenkinsfile") 
                     ? "${workDir}/${args.outputFile}" 
                     : args.outputFile
    def templateName = args.templateName

    def cliArgs = []
    cliArgs << "-c '${configFile}'"
    cliArgs << "-t '${templateName}'"
    cliArgs << "-o '${outputFile}'"

    if (args.branchName?.trim()) {
        cliArgs << "--set BRANCH_NAME=${args.branchName}"
    }
    if (args.buildTool?.trim()) {
        cliArgs << "--set BUILD_TOOL=${args.buildTool}"
    }
    if (args.buildSteps?.trim()) {
        cliArgs << "--set BUILD_STEPS=${args.buildSteps}"
    }
    if (args.repoUrl?.trim()) {
        cliArgs << "--set REPO_URL=${args.repoUrl}"
    }

    if (args.previewMode) {
        cliArgs << "--preview"
    }
    if (args.validateOnly) {
        cliArgs << "--validate-only"
    }
    if (args.strictMode) {
        cliArgs << "--strict"
    }

    echo "[INFO] Running pipeline-gen with arguments: ${cliArgs.join(' ')}"

    def output = sh(
        label: 'Generate Jenkinsfile',
        script: """
            mkdir -p "${workDir}/output"
            "${workDir}/bin/pipeline-gen" ${cliArgs.join(' ')} 2>&1
        """,
        returnStdout: true
    ).toString().trim()

    println(output)
    if (output.contains("Validation issues found")) {
        currentBuild.result = 'UNSTABLE'
        unstable("Validation issues found.")
    }
}
