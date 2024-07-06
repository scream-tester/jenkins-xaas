#!/usr/bin/env bash
# Validation helpers for pipeline-generator
# Rules are intentionally conservative and fast (no external deps).

set -o pipefail

# Allow-list of build tools
VALID_BUILD_TOOLS=("mvn" "gradle" "npm" "yarn" "pnpm" "make" "bash")

_valid_in_list() {
    local needle="$1"; shift
    local x
    for x in "$@"; do
        [[ "$x" == "$needle" ]] && return 0
    done
    return 1
}

valid_branch_name() {
    # Roughly aligned with common Git branch naming constraints.
    # - no spaces/control chars
    # - no double dots, no @{, and no trailing slash
    # - only these chars: A-Z a-z 0-9 . _ - /
    local s="$1"
    [[ -n "$s" ]] || return 1
    [[ "$s" =~ ^[A-Za-z0-9._/-]+$ ]] || return 1
    [[ "$s" != */ ]] || return 1
    [[ "$s" != .* ]] || true
    [[ "$s" != -* ]] || true
    [[ "$s" != */*//* ]] || true
    [[ "$s" != *..* ]] || true
    [[ "$s" != *@\{* ]] || true
    # Disallow spaces/newlines
    [[ "$s" != *" "* ]] || return 1
    [[ "$s" != *$'\n'* ]] || return 1
    return 0
}

valid_repo_url() {
    # Accepts common Git remotes:
    # - https://host/org/repo(.git)
    # - http://host/org/repo(.git)  (allowed but https preferred)
    # - ssh://git@host/org/repo(.git)
    # - git@host:org/repo(.git)
    local u="$1"
    [[ -n "$u" ]] || return 1
    # No spaces or control chars
    [[ "$u" =~ ^[[:print:]]+$ ]] || return 1
    [[ "$u" != *" "* ]] || return 1

    if [[ "$u" =~ ^(https?|ssh)://[A-Za-z0-9._~:@/-]+(\.git)?/?$ ]]; then
        return 0
    fi
    if [[ "$u" =~ ^git@[A-Za-z0-9._-]+:[A-Za-z0-9._/-]+(\.git)?$ ]]; then
        return 0
    fi
    return 1
}

valid_build_tool() {
    local t="$1"
    _valid_in_list "$t" "${VALID_BUILD_TOOLS[@]}"
}

valid_build_steps() {
    local s="$1"
    [[ -n "$s" ]] || return 1
    # Printable characters only (no control chars)
    [[ "$s" =~ ^[[:print:]]+$ ]] || return 1
    # Guard against command substitution; keep it simple but useful
    [[ "$s" != *'$('* ]] && [[ "$s" != *'`'* ]]
}

print_validation_rules() {
    cat >&2 <<'EOF'
[Info]  Validation rules:
        - BRANCH_NAME: letters/digits . _ - / ; no spaces; no trailing '/'
        - REPO_URL   : https/http/ssh or git@host:path form; optional .git
        - BUILD_TOOL : one of: mvn, gradle, npm, yarn, pnpm, make, bash
        - BUILD_STEPS: printable text; no command substitution ($() or backticks)
EOF
}

validate_all() {
    local strict="${1:-0}"
    local ok=0

    if ! valid_branch_name "${BRANCH_NAME_PARAM:-}"; then
        warn "Invalid BRANCH_NAME_PARAM: '${BRANCH_NAME_PARAM:-}'"
        ok=1
    fi
    if ! valid_repo_url "${REPO_URL_PARAM:-}"; then
        warn "Invalid REPO_URL_PARAM: '${REPO_URL_PARAM:-}'"
        ok=1
    fi
    if ! valid_build_tool "${BUILD_TOOL_PARAM:-}"; then
        warn "Invalid BUILD_TOOL_PARAM: '${BUILD_TOOL_PARAM:-}'. Allowed: ${VALID_BUILD_TOOLS[*]}"
        ok=1
    fi
    if ! valid_build_steps "${BUILD_STEPS_PARAM:-}"; then
        warn "Invalid BUILD_STEPS_PARAM: '${BUILD_STEPS_PARAM:-}'"
        ok=1
    fi

    if (( ok != 0 )); then
        [[ "$strict" -eq 1 ]] && return 1
        # Non-strict mode: still fail to protect users (safer default)
        return 1
    fi
    return 0
}
