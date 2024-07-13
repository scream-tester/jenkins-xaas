package vars

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*
import org.xaas.utils.JenkinsApiUtils
import java.net.URLEncoder

/**
 * Unit tests for the `deployClientJob` Jenkins shared library step.
 *
 * This test class mocks a Jenkins pipeline environment and verifies that:
 * 1. The client job is deployed successfully with required parameters.
 * 2. Proper error handling occurs when required parameters are missing.
 *
 * The test uses a mocked pipeline context (echo, httpRequest, libraryResource, error)
 * to avoid actual Jenkins API calls.
 */
class DeployClientJobTest {

    // Reference to the step under test
    def step

    // Tracks the sequence of pipeline calls
    def callStack

    /**
     * Setup method executed before each test.
     * - Initializes the callStack
     * - Loads the deployClientJob step
     * - Mocks pipeline methods and environment variables
     * - Mocks static methods like URLEncoder.encode
     */
    @Before
    void setUp() {
        callStack = []

        // Load the deployClientJob step from vars/
        step = new GroovyShell().parse(new File("vars/deployClientJob.groovy"))

        // Mock environment variables
        step.metaClass.env = [
            JENKINS_URL: "http://jenkins.local",
            JENKINS_USER: "testUser",
            JENKINS_API_TOKEN: "testToken",
            GITHUB_HTTPS_TOKEN: "dummy-token"
        ]

        // Mock pipeline methods
        step.metaClass.echo = { msg -> callStack << "echo:$msg" }
        step.metaClass.httpRequest = { args ->
            callStack << "httpRequest:${args.url}"
            if (args.url.contains("crumbIssuer")) return [content: "Jenkins-Crumb:12345"]
            if (args.url.contains("createItem")) return [content: "Job created"]
            if (args.url.contains("/build")) return [content: "Job triggered"]
            return [content: "ok"]
        }
        step.metaClass.libraryResource = { path ->
            callStack << "libraryResource:${path}"
            return "<xml>template for ${path}</xml>"
        }
        step.metaClass.error = { msg ->
            callStack << "error:${msg}"
            throw new IllegalStateException(msg)
        }

        // Mock static URLEncoder.encode
        URLEncoder.metaClass.static.encode = { str, encoding ->
            return str.replace(':', '%3A')
        }
    }

    /**
     * Positive test case: all required arguments provided
     * Verifies that the job template is read, job is created, and build is triggered.
     */
    @Test
    void testDeployClientJobSuccess() {
        step.call(clientName: "acme", repoUrl: "https://github.com/acme/repo.git")

        def callStackString = callStack.join('\n')

        // Assert that key steps were called
        assertTrue(callStackString.contains("libraryResource:job-deployer/job-templates/job-template.xml"))
        assertTrue(callStackString.contains("httpRequest:http://jenkins.local/createItem?name=acme-pipeline"))
        assertTrue(callStackString.contains("httpRequest:http://jenkins.local/job/acme-pipeline/build"))
        assertTrue(callStackString.contains("echo:[INFO]"))
    }

    /**
     * Negative test case: missing clientName
     * Verifies that an error is thrown and logged
     */
    @Test(expected = IllegalStateException)
    void testDeployClientJobMissingClientName() {
        step.call(repoUrl: "https://github.com/acme/repo.git")
        assertTrue(callStack.contains("error:[ERROR] clientName is required"))
    }

    /**
     * Negative test case: missing repoUrl
     * Verifies that an error is thrown and logged
     */
    @Test(expected = IllegalStateException)
    void testDeployClientJobMissingRepoUrl() {
        step.call(clientName: "acme")
        assertTrue(callStack.contains("error:[ERROR] repoUrl is required"))
    }
}
