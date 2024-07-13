package org.xaas.utils

/**
 * JenkinsApiUtils - Utility class for interacting with Jenkins via REST API.
 *
 * This class provides methods to:
 * 1. Secure API requests using a CSRF crumb (handled internally via a private method).
 * 2. Create new Jenkins jobs from XML definitions.
 * 3. Trigger Jenkins jobs.
 *
 * Authentication is handled via Jenkins API token, and `script.echo` is used for logging.
 * All HTTP requests include the crumb for CSRF protection.
 *
 * Usage:
 *   def jenkinsApiUtils = new JenkinsApiUtils(script)
 *   jenkinsApiUtils.createJob("my-job", xmlContent)
 *   jenkinsApiUtils.triggerJob("my-job")
 */
class JenkinsApiUtils implements Serializable {

    def script
    String authHeader

    /**
     * Constructor
     * @param script The pipeline script context
     */
    JenkinsApiUtils(script) {
        this.script = script
        // Prepare Base64 encoded auth header from Jenkins user and API token
        this.authHeader = "Basic " + "${script.env.JENKINS_USER}:${script.env.JENKINS_API_TOKEN}"
            .bytes.encodeBase64().toString()
        script.echo("JenkinsApiUtils initialized.")
    }

    /**
     * Fetch Jenkins Crumb for CSRF protection
     * @return Tuple of crumb header name and value
     */
    private def getCrumb() {
        def xpathQuery = URLEncoder.encode('concat(//crumbRequestField,":",//crumb)', 'UTF-8')
        def crumbUrl = "${script.env.JENKINS_URL}/crumbIssuer/api/xml?xpath=${xpathQuery}"

        script.echo("Fetching Jenkins crumb...")

        // Do not log response body to protect the crumb
        def response = script.httpRequest(
            url: crumbUrl,
            consoleLogResponseBody: false,
            customHeaders: [
                [name: 'Authorization', value: authHeader]
            ]
        )

        def crumbParts = response.content.trim().split(':')
        def crumbHeader = crumbParts[0]
        def crumbValue = crumbParts[1]

        script.echo("Crumb retrieved successfully")
        return [crumbHeader, crumbValue]
    }

    /**
     * Create a new Jenkins job
     * @param jobName Name of the job to create
     * @param xmlContent XML definition of the job
     */
    def createJob(String jobName, String xmlContent) {
        if (!jobName?.trim()) {
            throw new IllegalArgumentException("[ERROR] 'jobName' is required")
        }
        if (!xmlContent?.trim()) {
            throw new IllegalArgumentException("[ERROR] 'xmlContent' is required")
        }
        script.echo("Creating Jenkins job: ${jobName}...")

        def (crumbHeader, crumbValue) = getCrumb()

        script.httpRequest(
            httpMode: 'POST',
            url: "${script.env.JENKINS_URL}/createItem?name=${jobName}",
            requestBody: xmlContent,
            consoleLogResponseBody: false, // Hide XML content for security
            customHeaders: [
                [name: 'Content-Type', value: 'application/xml'],
                [name: 'Authorization', value: authHeader],
                [name: crumbHeader, value: crumbValue]
            ]
        )

        script.echo("Job ${jobName} created successfully.")
    }

    /**
     * Trigger an existing Jenkins job
     * @param jobName Name of the job to trigger
     */
    def triggerJob(String jobName) {
        script.echo("Triggering Jenkins job: ${jobName}...")

        def (crumbHeader, crumbValue) = getCrumb()

        script.httpRequest(
            httpMode: 'POST',
            url: "${script.env.JENKINS_URL}/job/${jobName}/build",
            consoleLogResponseBody: false, // Do not show crumb in logs
            customHeaders: [
                [name: 'Authorization', value: authHeader],
                [name: crumbHeader, value: crumbValue]
            ]
        )

        script.echo("Job ${jobName} triggered successfully.")
    }
}
