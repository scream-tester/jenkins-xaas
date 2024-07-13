package org.xaas.utils

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*

/**
 * JenkinsApiUtilsTest
 *
 * Unit tests for the JenkinsApiUtils class. This class validates Jenkins API operations
 * such as creating and triggering jobs via REST API in a Jenkins pipeline context.
 *
 * Tests are divided into:
 * 1. Positive tests: verifying correct API requests and expected behavior.
 * 2. Negative tests: simulating failures, invalid inputs, or unreachable API scenarios.
 *
 * Usage:
 *   Run these tests using Gradle or any IDE supporting JUnit:
 *     ./gradlew test
 *
 * Notes:
 *   - Jenkins environment variables are mocked to avoid real API calls.
 *   - Only two negative tests are included for relevance and maintainability.
 */
class JenkinsApiUtilsTest {

    // Mocked Jenkins pipeline script context
    def script
    // Call stack to capture mocked method invocations
    def callStack
    // Instance of the utility class under test
    JenkinsApiUtils utils

    /**
     * Setup executed before each test.
     * Initializes mocked script context and JenkinsApiUtils instance.
     */
    @Before
    void setUp() {
        callStack = []

        // Mocking pipeline environment and steps
        script = [
            env: [
                JENKINS_URL: "http://jenkins.local",
                JENKINS_USER: "testUser",
                JENKINS_API_TOKEN: "testToken"
            ],
            echo: { msg -> callStack << "echo:$msg" },
            httpRequest: { args ->
                callStack << "httpRequest:${args.url}"
                // Return a fake crumb when requesting crumbIssuer
                if (args.url.contains("crumbIssuer")) {
                    return [content: "Jenkins-Crumb:12345"]
                }
                return [content: "ok"]
            }
        ]

        // Initialize utility with mocked script
        utils = new JenkinsApiUtils(script)
    }

    // ==========================
    // Positive Test Cases
    // ==========================

    /**
     * Test creating a Jenkins job with valid inputs.
     * Verifies the pipeline echo messages and httpRequest calls.
     */
    @Test
    void testCreateJobSuccess() {
        utils.createJob("demo-job", "<xml>content</xml>")

        assertTrue(callStack.any { it.contains("Creating Jenkins job: demo-job") })
        assertTrue(callStack.any { it.contains("httpRequest:http://jenkins.local/createItem") })
        assertTrue(callStack.any { it.contains("Job demo-job created successfully.") })
    }

    /**
     * Test triggering a Jenkins job with valid inputs.
     * Verifies the pipeline echo messages and httpRequest calls.
     */
    @Test
    void testTriggerJobSuccess() {
        utils.triggerJob("demo-job")

        assertTrue(callStack.any { it.contains("Triggering Jenkins job: demo-job") })
        assertTrue(callStack.any { it.contains("httpRequest:http://jenkins.local/job/demo-job/build") })
        assertTrue(callStack.any { it.contains("Job demo-job triggered successfully.") })
    }

    // ==========================
    // Negative Test Cases
    // ==========================

    /**
     * Test creating a Jenkins job with an empty job name.
     * Expects IllegalArgumentException to be thrown.
     */
    @Test
    void testCreateJobWithEmptyName() {
        try {
            utils.createJob("", "<xml>content</xml>")
            fail("Expected IllegalArgumentException for empty jobName")
        } catch (IllegalArgumentException e) {
            assertTrue(e.message.contains("'jobName' is required"))
        }
    }

    /**
     * Test triggering a Jenkins job when the API is unreachable.
     * Overrides httpRequest to simulate a failure.
     */
    @Test
    void testHttpRequestFailureDuringTrigger() {
        // Simulate Jenkins API failure
        script.httpRequest = { args -> throw new RuntimeException("Jenkins API unreachable") }
        utils = new JenkinsApiUtils(script)

        try {
            utils.triggerJob("demo-job")
            fail("Expected RuntimeException for API failure")
        } catch (RuntimeException e) {
            assertTrue(e.message.contains("unreachable"))
        }
    }
}
