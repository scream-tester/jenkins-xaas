# Usage Guide

This document explains how to use the **Jenkins Pipeline Automation Framework** jobs and the parameters you can pass for different modes of operation.

---

## Job Parameters

The main Jenkins job (`Client Onboarding Master Job`) accepts the following parameters:

### 1. `PREVIEW_MODE` (Boolean)

- **Purpose:** Run the pipeline generator in preview mode without pushing the Jenkinsfile or creating the job in Jenkins.
- **Default:** `false`
- **Use case:** Quickly check what the generated Jenkinsfile will look like.
  
```text
true → Preview only, no push
false → Normal execution
```

---

### 2. `VALIDATE_ONLY` (Boolean)

- **Purpose:** Validate the configuration YAML without generating a Jenkinsfile or creating the job.
- **Default:** `false`
- **Use case:** Ensure the input configuration is correct before running the job.
  
```text
true → Only validate configuration
false → Does not generates Jenkinsfile and does not shows preview. Marks build Unstable
```

---

### 3. `STRICT_MODE` (Boolean)

- **Purpose:** Run the pipeline generator with strict validation. Fails the job if any warnings or non-critical issues exist.
- **Default:** `true`
- **Use case:** Ensure highest quality and strict compliance of generated pipelines.
  
```text
true → Strict validation, job fails on warnings
false → Non-strict, warnings do not fail job
```

---

## Running the Job

1. Open the **Client Onboarding Master Job** in Jenkins.
2. Set the parameters according to your need:

| Parameter        | Value | Description                       |
|------------------|-------|-----------------------------------|
| CLIENT_NAME      | string    | Name of the client (Required) |
| REPO_URL         | string    | Override repo URL (Required)  |
| CONFIG_FILE      | string    | Path to config file (.env or .yaml) (default: pipeline-generator/configs/sample_config.yaml) |
| TEMPLATE_NAME    | choice    | Select template from pipeline-generator/templates/ (default: basic) | 
| OUTPUT_FILE      | string    | Path to write generated Jenkinsfile (default: pipeline-generator/output/Jenkinsfile) |
| BRANCH_NAME      | string    | Override branch name (optional) |
| BUILD_TOOL       | string    | Override build tool (optional) |
| BUILD_STEPS      | string    | Override build steps (optional) |
| PREVIEW_MODE     | true/false | Preview generated Jenkinsfile (optional) |
| VALIDATE_ONLY    | true/false | Validate config only (optional) |
| STRICT_MODE      | true/false | Enable strict validation (optional) |

3. Click **Build with Parameters**.
4. Observe the console output for the results:
   - If `PREVIEW_MODE=true` → shows generated Jenkinsfile in terminal.
   - If `VALIDATE_ONLY=true` → only validates YAML configuration and marks build as Unstable if any warnings.
   - If `STRICT_MODE=true` → job fails if any warnings occur.

---

## Notes

- Always run with `PREVIEW_MODE=true` before first deployment for a new client.
- Use `VALIDATE_ONLY=true` if you update configuration but do not want to deploy immediately.
- `STRICT_MODE` is recommended for production deployments.
- To deploy pipeline job for client with newly generated Jenkinsfile run with `PREVIEW_MODE=false` and `VALIDATE_ONLY=false`
