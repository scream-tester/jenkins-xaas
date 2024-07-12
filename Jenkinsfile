/*
 * Jenkins pipeline to generate client-specific Jenkinsfiles
 * 
 * Features:
 * - Supports config/template-driven Jenkinsfile generation.
 * - Deploys generated pipeline to client repository and registers job in Jenkins.
 * 
 * Requirements:
 * - GitHub credentials (HTTPS or SSH).
 * - Shared library: jenkins-xaas-shared-lib.
 */

@Library('jenkins-xaas-shared-lib') _

pipeline {
    agent any

    // Input Parameters
    parameters {
        // Required parameters
        validatingString(
            name: 'CLIENT_NAME',
            defaultValue: '',
            regex: /^[\S ].*$/, // Must start with a non-space character
            failedValidationMessage: 'CLIENT_NAME cannot be empty',
            description: 'Unique client identifier (used in config path and job name)'
        )
        validatingString(
            name: 'REPO_URL',
            defaultValue: '',
            regex: /^[\S ].*$/, // Must start with a non-space character
            failedValidationMessage: 'REPO_URL cannot be empty',
            description: 'Target client repo URL.'
        )

        // Configuration and templates parameters
        string(
            name: 'CONFIG_FILE',
            defaultValue: 'configs/sample_config.yaml',
            description: 'Path to configuration file (.env or .yaml)'
        )
        choice(
            name: 'TEMPLATE_NAME',
            choices: ['basic', 'docker-node'],
            description: 'Template to use from pipeline-generator/templates/'
        )
        string(
            name: 'OUTPUT_FILE',
            defaultValue: 'output/Jenkinsfile',
            description: 'Path where generated Jenkinsfile will be written'
        )

        // Optional overrides
        string(name: 'BRANCH_NAME', defaultValue: '', description: 'Branch override (optional)')
        string(name: 'BUILD_TOOL', defaultValue: '', description: 'Build tool override (optional)')
        string(name: 'BUILD_STEPS', defaultValue: '', description: 'Custom build steps (optional)')

        // Flags
        booleanParam(name: 'PREVIEW_MODE', defaultValue: false, description: 'Preview only (no file write)')
        booleanParam(name: 'VALIDATE_ONLY', defaultValue: false, description: 'Validate inputs only (no generation)')
        booleanParam(
            name: 'STRICT_MODE',
            defaultValue: true,
            description: '''
                Enforce strict validation rules. <br/>
                <span style="color:red; font-weight:bold;">
                WARNING: If disabled, the Jenkinsfile will still be generated and job will deployed
                even if validation fails!
                </span>
            '''
        )
    }

    // Environment variables
    environment {
        // GitHub credentials:
        // - HTTPS token: used for cloning repos or deploying Jenkinsfiles to client repos.
        // - SSH token:   typically used for cloning repos.
        GITHUB_HTTPS_TOKEN        = "github-https-token"
        GITHUB_SSH_TOKEN          = "github-ssh-creds"

        // Path to Jenkinsfile generator inside checked-out repo
        JENKINSFILE_GENERATOR_DIR = "${WORKSPACE}/jenkins-xaas/pipeline-generator"
    }

    // Pipeline Stages
    stages {
        stage('Initialize') {
            steps {
                cleanWs()
                script {
                    // Set build metadata
                    currentBuild.displayName = "${params.CLIENT_NAME}"
                    currentBuild.description = "Generating Jenkinsfile for ${params.CLIENT_NAME}"
                }
            }
        }

        stage('Checkout Generator Repository') {
            steps {
                script {
                    /*
                    * Clone jenkins-xaas repo (contains generator + templates + job deployer).
                    * IMPORTANT:
                    * - The repo URL and credentials must match in style:
                    *   → If using SSH URL (`git@github.com:...`), use SSH credentials.
                    *   → If using HTTPS URL (`https://github.com/...`), use HTTPS token.
                    * - Mixing styles (SSH URL with HTTPS creds or vice versa) will fail.
                    */
                    cloneRepo(
                        url: '', // Configure forked repo URL
                        branch: '', // Configure branch
                        credsId: '', // Configure credsId according to URL style
                        targetDir: ''
                    )
                }
            }
        }

        stage('Generate Jenkinsfile') {
            steps {
                script {
                    // Run generator with provided params
                    generateJenkinsfile(
                        configFile: params.CONFIG_FILE,
                        outputFile: params.OUTPUT_FILE,
                        templateName: params.TEMPLATE_NAME,
                        branchName: params.BRANCH_NAME,
                        buildTool: params.BUILD_TOOL,
                        buildSteps: params.BUILD_STEPS,
                        repoUrl: params.REPO_URL,
                        previewMode: params.PREVIEW_MODE,
                        validateOnly: params.VALIDATE_ONLY,
                        strictMode: params.STRICT_MODE
                    )
                }
            }
            // Post Actions
            post {
                success {
                    echo("Jenkinsfile generated successfully.")
                }
                failure {
                    echo("Jenkinsfile generation failed. Check logs for details.")
                }
            }
        }

        stage('Deploy Jenkinsfile to GitHub') {
            when {
                expression { !(params.VALIDATE_ONLY || params.PREVIEW_MODE) }
            }
            steps {
                // Commit & push generated Jenkinsfile to client repo
                deployJenkinsfile(
                    repoUrl: params.REPO_URL,
                    clientName: params.CLIENT_NAME,
                    sourceFile: "${JENKINSFILE_GENERATOR_DIR}/${params.OUTPUT_FILE}",
                    targetBranch: '' // Configure branch in client repo to deploy Jenkinsfile
                )
            }
            // Post Actions
            post {
                success {
                    echo("Jenkinsfile deployed successfully.")
                }
                failure {
                    echo("Jenkinsfile deployment failed. Check logs for details.")
                }
            }
        }
    }
}
