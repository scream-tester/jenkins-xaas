# Architecture: Jenkins Pipeline Automation Framework

This document explains the design and architecture of the **Jenkins Pipeline Automation Framework**.
It highlights the main components, workflows, and interactions between the Pipeline Generator, Job Deployer, Jenkins, and client repositories on GitHub.

---

## 1. Overview

The framework is designed to automate client onboarding and pipeline creation in Jenkins.
It consists of two core components:

- **Pipeline Generator**
  - Reads client-specific configuration (`YAML`) / (`ENV`) and templates.
  - Generates a tailored `Jenkinsfile` for the client project.
  - Supports **PREVIEW_MODE**, **VALIDATE_ONLY**, and **STRICT_MODE** for safe testing and deploying.

- **Job Deployer**
  - Pushes generated `Jenkinsfile` to the client repository.
  - Creates the Jenkins job (`<CLIENT_NAME>-pipeline`).
  - Handles triggering initial builds and reporting status.

---

## 2. Workflow

1. **User/Developer Input**
   - Provide client configuration via YAML / ENV files.
   - Optionally choose one of the modes: `PREVIEW_MODE`, `VALIDATE_ONLY`, or `STRICT_MODE`.
   - Provide client configuration via parameters `BRANCH_NAME`, `BUILD_TOOLS`, `BUILD_STEPS`, `REPO_URL`

2. **Pipeline Generation**
   - Pipeline Generator reads configuration and templates.
   - Generates the `Jenkinsfile`.
   - In `PREVIEW_MODE` → outputs file to terminal without saving.
   - In `VALIDATE_ONLY` → checks syntax and template correctness.
   - In `STRICT_MODE` → enforces validation rules and fails on errors.

3. **Job Deployment**
   - Pushes the `Jenkinsfile` to the client repository.
   - Connects Jenkins to GitHub using credentials (SSH key or PAT token).
   - Creates the Jenkins job via the Jenkins API (requires API key).
   - Triggers the initial build.
   - Adds deployed `Jenkinsfile` URL and Jenkins Job URL to the build description.

4. **Client Repository**
   - Stores generated `Jenkinsfile` under `jenkins-xaas-client-pipelines/<CLIENT_NAME>/Jenkinsfile`.
   - Acts as the source of truth for the client pipeline.

---

## 3. Components & Interactions

| Component          | Responsibility                                                      |
|-------------------|---------------------------------------------------------------------|
| Pipeline Generator | Read config, apply templates, generate Jenkinsfile                  |
| Job Deployer       | Push Jenkinsfile, create/update Jenkins job, trigger initial build |
| Jenkins Server     | Hosts jobs, runs builds, stores logs                                 |
| Client Repo        | Stores the generated Jenkinsfile                                      |
| User/Developer     | Provides config and triggers pipeline                                |

**Sequence (text-based):**

1. Developer triggers Pipeline Generator with parameters : config + mode.
2. Generator previews & validates Jenkinsfile.
3. Generator outputs Jenkinsfile for deployment.
4. Job Deployer pushes Jenkinsfile to client repo.
5. Job Deployer creates Client Jenkins job.
6. Job Deployer triggers the first build automatically of Client Jenkins Job.

---

## 4. Notes

- All actions can be run manually or via the **Client Onboarding Master Job** in Jenkins.
- Modes (`PREVIEW_MODE`, `VALIDATE_ONLY`, `STRICT_MODE`) provide safe ways to test pipelines before full deployment.
- Credentials are securely stored in Jenkins; secrets are never committed to the repository.
- This framework is designed for **reuse across multiple clients**, minimizing manual setup and errors.

---

## 5. Repository Structure

```text
jenkins-xaas/
|-- build.gradle
|-- settings.gradle
|-- Jenkinsfile
|-- VERSION
|-- LICENSE
|-- CONTRIBUTING.md
|-- release.sh
|-- README.md
|-- docs/
|   |-- architecture.md
|   |-- plugins.md
|   |-- setup.md
|   |-- usage.md
|   `-- images/
|      |-- 01-devcontainer-jenkins-home.png
|      |-- 02-jenkins-plugins.png
|      |-- 03-jenkins-credentials.png
|      |-- 04-created-client-job.png
|      |-- 05-auto-created-client-job.png
|      `-- 06-auto-deployed-client-pipeline.png
|-- pipeline-generator/
|   |-- generator.sh
|   |-- bin/
|   |   `-- pipeline-gen
|   |-- configs/
|   |   |-- invalid_config.yaml
|   |   |-- sample_config.env
|   |   `-- sample_config.yaml
|   |-- lib/
|   |   |-- common.sh
|   |   `-- validate.sh
|   |-- templates/
|   |   |-- basic/
|   |   |   `-- Jenkinsfile.tmpl
|   |   `-- docker-node/
|   |       `-- Jenkinsfile.tmpl
|   `-- tests/
|       `-- smoke_test.sh
|-- resources/
|   `-- job-deployer/
|       `-- job-templates/
|           `-- job-template.xml
|-- src/
|   |-- org/
|   |   `-- xaas/
|   |        `-- utils/
|   |            |-- GitRepoUtils.groovy
|   |            `-- JenkinsApiUtils.groovy
|   `-- test/
|       `-- groovy/
|           |-- org/
|           |   `-- xaas/
|           |       `-- utils/
|           |           |-- GitRepoUtilsTest.groovy
|           |           `-- JenkinsApiUtilsTest.groovy
|           `-- vars/
|               |-- cloneRepoTest.groovy
|               |-- deployJenkinsfileTest.groovy
|               |-- deployClientJobTest.groovy
|               `-- generateJenkinsfileTest.groovy
`-- vars/
    |-- cloneRepo.groovy
    |-- deployJenkinsfile.groovy
    |-- deployClientJob.groovy
    `-- generateJenkinsfile.groovy
```
> Note: Additional standard files (`.gitignore`, `.gitattributes`, `.github/`) are present in the repo root but omitted here for brevity.
---

✅ **Outcome:**  
A developer or DevOps engineer can generate and deploy client-specific Jenkins pipelines quickly and safely without manually writing Jenkinsfiles or creating jobs from scratch.
