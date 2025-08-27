# Jenkins Pipeline Automation Framework
*(Repo: **jenkins-xaas** — Jenkins-as-a-Service for automating client pipelines with a Jenkinsfile generator and job deployer)*

## Overview
The **Jenkins Pipeline Automation Framework** provides a scalable way to generate and deploy client-specific Jenkins pipeline jobs.

It consists of two core components I built:
- **Pipeline Generator** → CLI tool (`pipeline-generator/`) for creating Jenkinsfiles from YAML / env configs and templates. Some generator logic is also exposed in the shared library for direct pipeline use.

- **Job Deployer** → implemented via Jenkins Shared Library structure (`vars/`, `src/`, `resources/`). Contains reusable steps, Groovy classes, and resources for deploying jobs, managing pipeline logic, and extending generator features inside Jenkins.

This enables teams to onboard new projects with **minimal manual Jenkins setup** while ensuring **consistency** across all jobs.

---
## Components & Setup

- **Pipeline Generator** (`pipeline-generator/`)
  - CLI tool for generating Jenkinsfiles dynamically from YAML/ env configs + templates.
  - See [docs/usage.md](docs/usage.md) for examples and options.

- **Job Deployer** (`vars/`, `src/`, `resources/`)
  - Implements the Job Deployer logic with reusable steps and Groovy classes.
  - See [docs/setup.md](docs/setup.md) for Jenkins configuration steps (Global Shared Library).

---

## Repository Structure (high-level)

```text
jenkins-xaas/
|-- docs/                 # Documentation (setup, usage, architecture, plugins)
|-- pipeline-generator/   # CLI tool for Jenkinsfile generation (bin/, configs/, templates/, tests/, lib/, generator.sh)
|-- resources/            # Static resources (job templates)
|-- src/                  # Groovy classes (namespaced under org.xaas.*)
|   |-- org/              # Library source code
|   `-- test/             # Unit tests for Groovy classes & shared library
|-- vars/                 # Global pipeline steps (Jenkins Shared Library entrypoints)
|-- Jenkinsfile           # Top-level pipeline entry
|-- build.gradle          # Gradle build config
|-- settings.gradle       # Gradle settings
|-- release.sh            # Release automation script
|-- VERSION               # Version metadata
|-- LICENSE               # License file
`-- README.md             # Project overview
```
> Note: Additional standard files (`CONTRIBUTING.md`, `.gitignore`, `.gitattributes`, `.github/`) are present in the repo root but omitted here for brevity.
For detailed repository structure, refer to **[docs/architecture.md](docs/architecture.md)**.
---

---

## Quick Start

Open this repository in **GitHub Codespaces** (or VS Code Remote-Containers).
Once the **GitHub Codespaces** is fully loaded, a forwarded port (`8080`) will appear in the **Ports** tab.
Click the link or copy it into your browser — this is your Jenkins URL.

Follow [docs/setup.md](docs/setup.md) to complete the initial Jenkins setup.

Once Jenkins is running:

```bash
# Run smoke tests
cd pipeline-generator
./tests/smoke_test.sh

# Preview generator output
cd pipeline-generator
./bin/pipeline-gen -c ./configs/sample_config.yaml -t docker-node --preview
```

---
## Documentation

- **Setup & Configuration** → [docs/setup.md](docs/setup.md)
- **Job Parameters & Usage** → [docs/usage.md](docs/usage.md)
- **Architecture Overview** → [docs/architecture.md](docs/architecture.md)
- **Required Plugins** → [docs/plugins.md](docs/plugins.md)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on contributing, running tests, and maintaining coding standards.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.