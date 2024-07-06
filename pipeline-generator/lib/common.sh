#!/usr/bin/env bash
# Common shell helpers for pipeline-generator

set -o pipefail

die() {
    echo "[Error] $*" >&2
    exit 1
}

info() {
    # Send info logs to stderr so stdout can be used for clean previews
    echo "[Info]  $*" >&2
}

warn() {
  echo "[Warn]  $*" >&2
}

# Escape value for safe sed replacement with '|' delimiter.
# Escapes: backslash and '&' (sed replacement special), and pipe if used.
escape_for_sed() {
    # 1) escape backslashes
    # 2) escape &
    # 3) escape |
    printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/&/\\\&/g' -e 's/|/\\|/g'
}
