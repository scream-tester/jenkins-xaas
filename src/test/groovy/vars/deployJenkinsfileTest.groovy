package vars

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*
import org.xaas.utils.GitRepoUtils
import java.io.File
import groovy.mock.interceptor.StubFor

/**
 * Unit tests for the `deployJenkinsfile` Jenkins shared library step.
 *
 * This test class mocks a Jenkins pipeline environment and verifies that:
 * 1. The Jenkinsfile is deployed successfully with required parameters.
 * 2. Proper error handling occurs when required parameters are missing.
 *
 * The test uses a mocked pipeline context (echo, sh, cloneRepo, error) and
 * a mocked GitRepoUtils instance to isolate the step from actual Git operations.
 */
class DeployJenkinsfileTest {

    // Reference to the step under test
    def step

    // Tracks the sequence of pipeline calls
    def callStack

    /**
     * Setup method executed before each test.
     * - Initializes the callStack
     * - Loads the deployJenkinsfile step
     * - Mocks pipeline methods and environment variables
     * - Mocks the cloneRepo step
     * - Mocks the GitRepoUtils instance
     */
    @Before
    void setUp() {
        callStack = []

        // Load the deployJenkinsfile step from vars/
        step = new GroovyShell().parse(new File("vars/deployJenkinsfile.groovy"))

        // Mock environment variables
        step.metaClass.env = [
            GITHUB_HTTPS_TOKEN: "dummy-token"
        ]

        // Mock pipeline methods
        step.metaClass.echo = { msg -> callStack << "echo:${msg}" }
        step.metaClass.sh = { args ->
            callStack << "sh:${args.label}"
            callStack << "sh-script:${args.script}"
        }
        step.metaClass.error = { msg ->
            callStack << "error:${msg}"
            throw new IllegalStateException(msg)
        }

        // Mock the cloneRepo step (used internally by deployJenkinsfile)
        step.metaClass.cloneRepo = { args ->
            callStack << "cloneRepo:url=${args.url},branch=${args.branch},credsId=${args.credsId},targetDir=${args.targetDir}"
        }

        // Mock GitRepoUtils instance to isolate real Git operations
        GitRepoUtils.metaClass.constructor = { script ->
            return [
                commitAndPush: { repoUrl, commitMsg, targetBranch, workDir, credsId ->
                    callStack << "gitRepoUtils.commitAndPush:repoUrl=${repoUrl},commitMsg=${commitMsg},targetBranch=${targetBranch},workDir=${workDir},credsId=${credsId}"
                }
            ]
        }
    }

    /**
     * Positive test case: all required arguments provided
     * Verifies that the Jenkinsfile is cloned, copied, and committed correctly.
     */
    @Test
    void testDeployJenkinsfileSuccess() {
        def args = [
            repoUrl: "https://github.com/myorg/client-repo.git",
            targetBranch: "main",
            clientName: "AcmeCorp",
            sourceFile: "./templates/Jenkinsfile"
        ]

        step.call(args)

        def callStackString = callStack.join('\n')

        // Regex to match the shell copy command
        def shScriptRegex = ~/(?s)sh-script:.*cd "jenkins-xaas-client-repo".*mkdir -p "AcmeCorp".*cp "\.\/templates\/Jenkinsfile" "AcmeCorp\/Jenkinsfile"/

        // Assert that key steps were executed correctly
        assertTrue(callStackString.contains("echo:[INFO] Cloning repository"))
        assertTrue(callStackString.contains("cloneRepo:url=https://github.com/myorg/client-repo.git,branch=main,credsId=dummy-token,targetDir=jenkins-xaas-client-repo"))
        assertTrue(callStackString.contains("sh:Copying Jenkinsfile into jenkins-xaas-client-repo/AcmeCorp"))
        assertTrue((callStackString =~ shScriptRegex) as boolean)
        assertTrue(callStackString.contains("gitRepoUtils.commitAndPush:repoUrl=https://github.com/myorg/client-repo.git,commitMsg=Add/Update Jenkinsfile for client AcmeCorp,targetBranch=main,workDir=jenkins-xaas-client-repo,credsId=dummy-token"))
        assertTrue(callStackString.contains("echo:[SUCCESS] Jenkinsfile deployed successfully"))
    }

    /**
     * Negative test cases: Missing required arguments
     * Verifies that an error is thrown and logged when arguments are missing.
     */
    @Test(expected = IllegalStateException)
    void testDeployJenkinsfileMissingRepoUrl() {
        def args = [
            targetBranch: "main",
            clientName: "AcmeCorp",
            sourceFile: "./templates/Jenkinsfile"
        ]
        step.call(args)
        assertTrue(callStack.join('\n').contains("error:[ERROR] repoUrl is required"))
    }

    @Test(expected = IllegalStateException)
    void testDeployJenkinsfileMissingTargetBranch() {
        def args = [
            repoUrl: "https://github.com/myorg/client-repo.git",
            clientName: "AcmeCorp",
            sourceFile: "./templates/Jenkinsfile"
        ]
        step.call(args)
        assertTrue(callStack.join('\n').contains("error:[ERROR] targetBranch is required"))
    }

    @Test(expected = IllegalStateException)
    void testDeployJenkinsfileMissingClientName() {
        def args = [
            repoUrl: "https://github.com/myorg/client-repo.git",
            targetBranch: "main",
            sourceFile: "./templates/Jenkinsfile"
        ]
        step.call(args)
        assertTrue(callStack.join('\n').contains("error:[ERROR] clientName is required"))
    }

    @Test(expected = IllegalStateException)
    void testDeployJenkinsfileMissingSourceFile() {
        def args = [
            repoUrl: "https://github.com/myorg/client-repo.git",
            targetBranch: "main",
            clientName: "AcmeCorp"
        ]
        step.call(args)
        assertTrue(callStack.join('\n').contains("error:[ERROR] sourceFile is required"))
    }
}
