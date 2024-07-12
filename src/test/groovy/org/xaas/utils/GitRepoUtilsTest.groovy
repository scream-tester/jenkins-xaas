package org.xaas.utils

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*

/**
 * GitRepoUtilsTest
 *
 * Unit tests for the GitRepoUtils class. This class validates core Git operations
 * in a Jenkins Pipeline context, including repository cloning, commit, and push.
 *
 * Tests are divided into:
 * 1. Positive tests: verifying expected behavior for valid inputs.
 * 2. Negative tests: validating behavior when errors occur (e.g., invalid inputs or script failures).
 *
 * Usage:
 *   Run these tests using Gradle or any IDE supporting JUnit:
 *     ./gradlew test
 *
 * Note:
 *   - Jenkins pipeline steps are mocked to avoid actual Git/SSH operations.
 *   - Only two negative tests are included for relevance and maintainability.
 */
class GitRepoUtilsTest {

    // Mocked Jenkins pipeline script context
    def script
    // Instance of the utility class under test
    GitRepoUtils utils
    // Call stack to capture mocked method invocations
    def callStack

    /**
     * Setup executed before each test.
     * Initializes the mocked script and GitRepoUtils instance.
     */
    @Before
    void setUp() {
        callStack = []

        // Mocking Jenkins pipeline methods to capture calls
        script = [
            echo: { msg -> callStack << "echo:$msg" },
            dir : { dir, Closure c -> callStack << "dir:$dir"; c() },
            git : { args -> callStack << "git:$args" },
            withCredentials: { creds, Closure c -> callStack << "withCreds"; c() },
            usernamePassword: { m -> return m },
            sh  : { m -> callStack << "sh:${m.script}" }
        ]

        // Initialize the utility with the mocked script
        utils = new GitRepoUtils(script)
    }

    // ==========================
    // Positive Test Cases
    // ==========================

    /**
     * Test cloning a repository with valid inputs.
     * Verifies that the appropriate pipeline steps (echo, dir, git) are called.
     */
    @Test
    void testCloneRepo() {
        utils.cloneRepo("https://github.com/test/repo.git", "main", "credsId", "workspace")

        assertTrue(callStack.any { it.contains("Cloning repository https://github.com/test/repo.git") })
        assertTrue(callStack.any { it.contains("dir:workspace") })
        assertTrue(callStack.any { it.contains("git:") })
    }

    /**
     * Test committing and pushing changes with valid inputs.
     * Verifies that withCredentials and shell script steps are invoked.
     */
    @Test
    void testCommitAndPush() {
        utils.commitAndPush("https://github.com/test/repo.git", "test commit", "main", "workspace", "credsId")

        assertTrue(callStack.any { it.contains("withCreds") })
        assertTrue(callStack.any { it.contains("sh:") })
    }

    // ==========================
    // Negative Test Cases
    // ==========================

    /**
     * Test cloning a repository with an empty URL.
     * Expects an IllegalArgumentException to be thrown.
     */
    @Test
    void testCloneRepoWithEmptyUrl() {
        try {
            utils.cloneRepo("", "main", "credsId", "workspace")
            fail("Expected IllegalArgumentException for empty URL")
        } catch (IllegalArgumentException e) {
            assertTrue(e.message.contains("'url' is required"))
        }
    }

    /**
     * Test committing and pushing changes when a script failure occurs.
     * Overrides the shell step to throw a RuntimeException.
     * Expects the exception to propagate.
     */
    @Test
    void testScriptExceptionDuringCommit() {
        // Simulate shell command failure
        script.sh = { m -> throw new RuntimeException("Shell command failed") }

        try {
            utils.commitAndPush("https://github.com/test/repo.git", "test commit", "main", "workspace", "credsId")
            fail("Expected exception due to shell command failure")
        } catch (RuntimeException e) {
            assertEquals("Shell command failed", e.message)
        }
    }
}
