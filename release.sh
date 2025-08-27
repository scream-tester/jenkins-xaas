#!/bin/bash
set -euo pipefail

# ===============================
# Jenkins Pipeline Automation Framework Release Script
# ===============================
# Usage:
#   ./release.sh [patch|minor|major|vX.Y.Z] [--tag]
# Examples:
#   ./release.sh           # defaults to patch
#   ./release.sh minor     # increments minor version
#   ./release.sh v1.0.0    # sets exact version
#   ./release.sh --tag     # tag current VERSION on main
# Requirements:
#   - git CLI configured
#   - GitHub CLI (gh) authenticated
#   - No direct pushes to main
# ===============================

VERSION_FILE="VERSION"
DEFAULT_BRANCH="main"

# -------------------------------
# Helper functions
# -------------------------------

get_current_version() {
    if [[ -f "$VERSION_FILE" ]]; then
        cat "$VERSION_FILE"
    else
        echo "0.0.0"
    fi
}

increment_version() {
    local version=$1
    local type=$2

    IFS='.' read -r major minor patch <<< "${version#v}"

    case "$type" in
        patch)
            patch=$((patch+1));;
        minor)
            minor=$((minor+1))
            patch=0;;
        major)
            major=$((major+1))
            minor=0
            patch=0;;
        *)
            echo "$type"  # explicit version passed
            return;;
    esac

    echo "$major.$minor.$patch"
}

# -------------------------------
# Parse argument
# -------------------------------

ARG=${1:-patch}
TAG_ONLY=false

if [[ "$ARG" == "--tag" ]]; then
    TAG_ONLY=true
    NEW_VERSION=$(get_current_version)
elif [[ $ARG =~ ^v?[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    NEW_VERSION="${ARG#v}"
else
    CURRENT_VERSION=$(get_current_version)
    NEW_VERSION=$(increment_version "$CURRENT_VERSION" "$ARG")
fi

RELEASE_BRANCH="release/v$NEW_VERSION"

# -------------------------------
# Tagging workflow
# -------------------------------
if [ "$TAG_ONLY" = true ]; then
    echo "[INFO] Tagging version $NEW_VERSION on $DEFAULT_BRANCH..."
    git checkout "$DEFAULT_BRANCH"
    git pull origin "$DEFAULT_BRANCH"
    git tag -a "$NEW_VERSION" -m "Release $NEW_VERSION"
    git push origin "$NEW_VERSION"
    echo "[SUCCESS] Tagged $NEW_VERSION on $DEFAULT_BRANCH."
    exit 0
fi

# -------------------------------
# Create release branch
# -------------------------------
echo "[INFO] Creating release branch $RELEASE_BRANCH from $DEFAULT_BRANCH..."
git fetch origin "$DEFAULT_BRANCH"
git checkout -b "$RELEASE_BRANCH" "origin/$DEFAULT_BRANCH"

# -------------------------------
# Update VERSION file
# -------------------------------
echo "v$NEW_VERSION" > "$VERSION_FILE"
git add "$VERSION_FILE"
git commit -m "chore(release): bump version to $NEW_VERSION"

# -------------------------------
# Push release branch
# -------------------------------
echo "[INFO] Pushing release branch to remote..."
git push -u origin "$RELEASE_BRANCH"

# -------------------------------
# Open Pull Request using GitHub CLI
# -------------------------------
echo "[INFO] Creating Pull Request to $DEFAULT_BRANCH..."
gh pr create \
    --title "Release v$NEW_VERSION" \
    --body "This PR bumps version to v$NEW_VERSION." \
    --base "$DEFAULT_BRANCH" \
    --head "$RELEASE_BRANCH"

echo "[SUCCESS] Release branch '$RELEASE_BRANCH' created and PR opened against $DEFAULT_BRANCH."
echo "[INFO] After PR merge, run './release.sh --tag' to tag v$NEW_VERSION."
