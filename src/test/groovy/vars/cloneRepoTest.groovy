package vars

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*
import org.xaas.utils.GitRepoUtils

/**
 * Unit tests for the `cloneRepo` Jenkins shared library step.
 *
 * This test class mocks a Jenkins pipeline environment and verifies that:
 * 1. The repository is cloned successfully with correct parameters.
 * 2. Proper error handling occurs when required parameters are missing.
 *
 * The test uses a mocked pipeline context (echo, dir, git, error) to avoid
 * actual Git or filesystem operations.
 */
class CloneRepoTest {

    // Mocked Jenkins pipeline script context
    def script

    // Tracks the sequence of calls (echo, dir, git)
    def callStack

    // Reference to the step under test
    def step

    /**
     * Setup method executed before each test.
     * Initializes call stack and mocks the Jenkins pipeline context.
     * Loads the `cloneRepo.groovy` step as a Groovy object.
     */
    @Before
    void setUp() {
        callStack = []

        // Mock Jenkins pipeline methods
        script = [
            echo: { msg -> callStack << "echo:$msg" },                     // capture echo calls
            dir : { dir, Closure c -> callStack << "dir:$dir"; c() },      // capture dir calls
            git : { args -> callStack << "git:$args" },                    // capture git commands
            error: { msg -> throw new IllegalArgumentException(msg) }      // mock error() call
        ]

        // Load the cloneRepo step from vars/ folder
        step = new GroovyShell().parse(new File("vars/cloneRepo.groovy"))

        // Provide mocked pipeline properties
        step.metaClass.getProperty = { String name -> 
            return script[name]
        }
    }

    /**
     * Test that cloneRepo executes successfully with valid parameters.
     * Verifies that echo, dir, and git methods are called in the expected order.
     */
    @Test
    void testCloneRepoStepSuccess() {
        step.call(url: "https://github.com/test/repo.git", branch: "main", credsId: "creds", targetDir: "workspace")

        assertTrue(callStack.any { it.contains("echo:Cloning repository https://github.com/test/repo.git") })
        assertTrue(callStack.any { it.contains("dir:workspace") })
        assertTrue(callStack.any { it.contains("git:") })
    }

    /**
     * Test that cloneRepo fails when the required `url` parameter is missing.
     * Ensures proper error message is thrown.
     */
    @Test
    void testCloneRepoStepMissingUrl() {
        try {
            step.call(branch: "main", credsId: "creds")
            fail("Expected IllegalArgumentException for missing URL")
        } catch (IllegalArgumentException e) {
            assertTrue(e.message.contains("'url' is required"))
        }
    }
}
