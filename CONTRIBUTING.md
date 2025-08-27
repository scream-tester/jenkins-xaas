# Contributing to Jenkins Pipeline Automation Framework

Thank you for your interest in contributing! While this is primarily a portfolio project, it also demonstrates professional DevOps practices. Contributions are welcome for improvements, bug fixes, or documentation updates.
This guide explains how to set up your environment, make changes, and submit a pull request (PR).

---

## 📌 How to Contribute

1. **Fork the repository**
   - Click the **Fork** button on the top right of this repo.
   - This creates your own copy under your GitHub account.
   - Enter `Project-Name` for your fork.

2. **Clone your fork locally**
   - Open your terminal and run the following commands. Remember to replace `<YOUR_GITHUB_USERNAME>` with your actual GitHub username and `<Project-Name>` with your Project Name
   ```bash
   git clone https://github.com/<YOUR_GITHUB_USERNAME>/<Project-Name>.git
   cd <Project-Name>
   ```

3. **Create a new branch**
   - Use a descriptive branch name:
     ```bash
     git checkout -b feature/add-ci-workflow
     ```
    - Examples:
      - `feature/add-ci-workflow`
      - `fix/typo-in-readme`
      - `docs/update-architecture`

4. **Make your changes**
   - Follow coding style guidelines.
   - Add/update tests if relevant.
   - Keep commits clean & meaningful.

5. **Tests**
   - Add new tests under `pipeline-generator/tests/` if applicable.
   - Ensure existing Smoke tests pass:
     ```bash
     ./pipeline-generator/tests/smoke_test.sh
     ```
   - Ensure all Gradle tests pass:
     ```bash
     ./gradlew test
     ```

6. **Push your branch**
    ```bash
    git push origin feature/add-ci-workflow
    ```

7. **Open a Pull Request**
   - Go to your fork on GitHub, click **New Pull Request**.
   - Base branch = `main` of this repo.
   - Compare branch = your feature branch.
   - Fill out PR description with:
     - What you changed
     - Why this change is needed
     - Any related issues

---

## Pull Request Guidelines
- Keep PRs focused (1 feature/bugfix per PR).
- Make sure the branch is up to date with `main`.
- Use [Conventional Commits](https://www.conventionalcommits.org/) style if possible:
  - `feat: add dynamic config support`
  - `fix: correct path in deploy script`
  - `docs: update contributing guide`

---

## Maintainer Workflow
- Review PRs for correctness, style, and tests.
- Merge with **Rebase & Merge** to keep history clean.
- Release process:
  - Open a release PR with:
    ```bash
    ./release.sh major | minor | patch | v*.*.*   # default: patch
    ```
  - After merging, tag the release:
    ```bash
    ./release.sh --tag
    ```
    Pushing a `v*.*.*` tag to `main` triggers GitHub Actions to publish the release automatically.

---

## Code style

- Shell scripts: use `set -euo pipefail`.
- Keep templates and Jenkinsfiles readable.
- Keep changes small and focused—one feature or bugfix per PR.

---

## Security

- Do not commit secrets or sensitive information.
- Use Jenkins credentials or environment variables for any secrets.

---

## License

By contributing, you agree that your contributions will be licensed under the same license as this project (see `LICENSE`).

---

## Questions?
If you have any questions, feel free to open an issue or start a discussion.