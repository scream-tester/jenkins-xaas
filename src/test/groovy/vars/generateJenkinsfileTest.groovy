package vars

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*
import java.io.File
import groovy.mock.interceptor.StubFor

/**
 * Unit tests for the `generateJenkinsfile` Jenkins shared library step.
 *
 * This test class mocks a Jenkins pipeline environment and verifies:
 * 1. Jenkinsfile generation executes correctly with all required and optional arguments.
 * 2. Proper error handling occurs when required arguments (configFile, outputFile, templateName) are missing.
 *
 * All pipeline methods (echo, sh, error) are mocked on the step's metaClass.
 */
class GenerateJenkinsfileTest {

    // Reference to the step under test
    def step

    // Tracks the sequence of pipeline calls
    def callStack

    /**
     * Setup executed before each test:
     * - Initializes callStack
     * - Loads the generateJenkinsfile step from vars/
     * - Mocks env variables and pipeline methods (echo, sh, error)
     */
    @Before
    void setUp() {
        callStack = []

        // Load the generateJenkinsfile step
        step = new GroovyShell().parse(new File("vars/generateJenkinsfile.groovy"))
        
        // Inject mock environment variable
        step.metaClass.env = [
            JENKINSFILE_GENERATOR_DIR: "/mock/generator"
        ]

        // Mock Jenkins pipeline methods
        step.metaClass.echo = { msg -> callStack << "echo:${msg}" }
        step.metaClass.sh = { args ->
            callStack << "sh:${args.label}"
            callStack << "sh-script:${args.script}"
        }
        step.metaClass.error = { msg ->
            callStack << "error:${msg}"
            throw new IllegalStateException(msg)
        }
    }

    /**
     * Positive test: all required and optional arguments provided
     * Verifies that the Jenkinsfile is generated and shell command is called correctly.
     */
    @Test
    void testGenerateJenkinsfileSuccess() {
        def args = [
            configFile: "configs/app-config.yaml",
            outputFile: "output/Jenkinsfile",
            templateName: "java-maven",
            branchName: "main",
            buildTool: "maven",
            buildSteps: "clean install",
            repoUrl: "https://github.com/org/repo.git",
            previewMode: true,
            validateOnly: false,
            strictMode: true
        ]
        
        step.call(args)
        def callStackString = callStack.join('\n')

        def expectedSnippets = [
            "echo:[INFO] Running pipeline-gen with arguments",
            "sh:Generate Jenkinsfile",
            "sh-script:",
            "mkdir -p \"/mock/generator/output\"",
            "/mock/generator/bin/pipeline-gen",
            "-c '/mock/generator/configs/app-config.yaml'",
            "-t 'java-maven'",
            "-o '/mock/generator/output/Jenkinsfile'",
            "--set BRANCH_NAME=main",
            "--set BUILD_TOOL=maven",
            "--set BUILD_STEPS=clean install",
            "--set REPO_URL=https://github.com/org/repo.git",
            "--preview",
            "--strict"
        ]

        expectedSnippets.each { snippet ->
            assertTrue(callStackString.contains(snippet))
        }
    }
    
    /**
     * Negative test: missing required argument `configFile`
     */
    @Test(expected = IllegalStateException)
    void testGenerateJenkinsfileMissingConfigFile() {
        def args = [
            outputFile: "output/Jenkinsfile",
            templateName: "java-maven"
        ]
        try {
            step.call(args)
        } catch (IllegalStateException e) {
            def callStackString = callStack.join('\n')
            assertTrue(callStackString.contains("error:[ERROR] configFile is required and cannot be null or empty"))
            throw e
        }
    }

    /**
     * Negative test: missing required argument `outputFile`
     */
    @Test(expected = IllegalStateException)
    void testGenerateJenkinsfileMissingOutputFile() {
        def args = [
            configFile: "configs/app-config.yaml",
            templateName: "java-maven"
        ]
        try {
            step.call(args)
        } catch (IllegalStateException e) {
            def callStackString = callStack.join('\n')
            assertTrue(callStackString.contains("error:[ERROR] outputFile is required and cannot be null or empty"))
            throw e
        }
    }

    /**
     * Negative test: missing required argument `templateName`
     */
    @Test(expected = IllegalStateException)
    void testGenerateJenkinsfileMissingTemplateName() {
        def args = [
            configFile: "configs/app-config.yaml",
            outputFile: "output/Jenkinsfile"
        ]
        try {
            step.call(args)
        } catch (IllegalStateException e) {
            def callStackString = callStack.join('\n')
            assertTrue(callStackString.contains("error:[ERROR] templateName is required and cannot be null or empty"))
            throw e
        }
    }
}
