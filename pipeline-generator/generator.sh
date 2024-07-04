#!/usr/bin/env bash

set -euo pipefail

# --- Constants / Defaults ---
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
LIB_DIR="$ROOT_DIR/lib"
TEMPLATES_DIR="$ROOT_DIR/templates"
OUTPUT_DIR="$ROOT_DIR/output"
CONFIGS_DIR="$ROOT_DIR/configs"

DEFAULT_ENV_CONFIG="$CONFIGS_DIR/sample_config.env"
DEFAULT_YAML_CONFIG="$CONFIGS_DIR/sample_config.yaml"
DEFAULT_TEMPLATE_PACK="basic"
DEFAULT_TEMPLATE_FILE="Jenkinsfile.tmpl"

# Allow users to influence behavior without CLI
# CONFIG_FILE: path to .env or .yaml
# TEMPLATE_NAME: template pack name (folder under templates/)
CONFIG_FILE="${CONFIG_FILE:-}"
TEMPLATE_NAME="${TEMPLATE_NAME:-$DEFAULT_TEMPLATE_PACK}"

# --- Helpers ---
# shellcheck source=lib/common.sh
source "$LIB_DIR/common.sh"

require_file() { [[ -f "$1" ]] || die "File not found: $1"; }
require_dir()  { [[ -d "$1" ]] || die "Directory not found: $1"; }

# --- Loader functions ---
load_env_config() {
    local file="$1"
    info "Loading .env config: $file"
    # shellcheck disable=SC1090
    source "$file"
}

yaml_get_scalar() {
    # Extract top-level key's scalar value from YAML (no arrays, no nesting).
    # Trims surrounding quotes and leading spaces after colon.
    local key="$1" file="$2"
    awk -v k="$key" -F': *' '
        $1 == k {
            val=$2
            gsub(/^[ \t]+|[ \t]+$/,"",val)
            # remove surrounding single/double quotes if present
            if (val ~ /^".*"$/ || val ~ /^'\''.*'\''$/) {
            sub(/^["'\'']/, "", val); sub(/["'\'']$/, "", val)
            }
            print val
            exit
        }
    ' "$file"
}

load_yaml_config() {
    local file="$1"
    info "Loading YAML config: $file"
    BRANCH_NAME_PARAM="$(yaml_get_scalar 'BRANCH_NAME_PARAM' "$file")"
    BUILD_TOOL_PARAM="$(yaml_get_scalar 'BUILD_TOOL_PARAM' "$file")"
    BUILD_STEPS_PARAM="$(yaml_get_scalar 'BUILD_STEPS_PARAM' "$file")"
    REPO_URL_PARAM="$(yaml_get_scalar 'REPO_URL_PARAM' "$file")"
}

autodetect_config() {
    if [[ -n "$CONFIG_FILE" ]]; then
        require_file "$CONFIG_FILE"
        case "$CONFIG_FILE" in
            *.env)  load_env_config "$CONFIG_FILE" ;;
            *.yml|*.yaml) load_yaml_config "$CONFIG_FILE" ;;
            *) die "Unsupported config format (use .env or .yaml): $CONFIG_FILE" ;;
        esac
        return
    fi

    # No CONFIG_FILE provided; try .env, then .yaml
    if [[ -f "$DEFAULT_ENV_CONFIG" ]]; then
        load_env_config "$DEFAULT_ENV_CONFIG"
    elif [[ -f "$DEFAULT_YAML_CONFIG" ]]; then
        load_yaml_config "$DEFAULT_YAML_CONFIG"
    else
        die "No default config found. Provide CONFIG_FILE or create $DEFAULT_ENV_CONFIG / $DEFAULT_YAML_CONFIG"
    fi
}

apply_env_overrides() {
    # Allow environment variables to override config file values
    # (useful for Jenkins parameters)
    if [[ -n "${BRANCH_NAME:-}" ]]; then BRANCH_NAME_PARAM="$BRANCH_NAME"; fi
    if [[ -n "${BUILD_TOOL:-}"  ]]; then BUILD_TOOL_PARAM="$BUILD_TOOL";   fi
    if [[ -n "${BUILD_STEPS:-}"       ]]; then BUILD_STEPS_PARAM="$BUILD_STEPS";             fi
    if [[ -n "${REPO_URL:-}"     ]]; then REPO_URL_PARAM="$REPO_URL"; fi
}

validate_required() {
    local missing=()
    for v in BRANCH_NAME_PARAM REPO_URL_PARAM BUILD_TOOL_PARAM BUILD_STEPS_PARAM; do
        if [[ -z "${!v:-}" ]]; then missing+=("$v"); fi
    done
    if (( ${#missing[@]} > 0 )); then
        die "Missing required variables: ${missing[*]}"
    fi
}

select_template() {
    local pack="$1"
    local file="$TEMPLATES_DIR/$pack/$DEFAULT_TEMPLATE_FILE"
    require_dir "$TEMPLATES_DIR/$pack"
    require_file "$file"
    TEMPLATE_FILE="$file"
    info "Using template: $TEMPLATE_FILE"
}

generate() {
    mkdir -p "$OUTPUT_DIR"
    local out="$OUTPUT_DIR/Jenkinsfile"

    # Escape values for safe sed replacement
    local esc_branch esc_tool esc_BUILD_STEPS_PARAM
    esc_branch="$(escape_for_sed "${BRANCH_NAME_PARAM}")"
    esc_tool="$(escape_for_sed   "${BUILD_TOOL_PARAM}")"
    esc_BUILD_STEPS_PARAM="$(escape_for_sed  "${BUILD_STEPS_PARAM}")"
    esc_repo="$(escape_for_sed   "${REPO_URL_PARAM}")"

    sed -e "s|\${BRANCH_NAME_PARAM}|${esc_branch}|g" \
        -e "s|\${BUILD_TOOL_PARAM}|${esc_tool}|g" \
        -e "s|\${BUILD_STEPS_PARAM}|${esc_BUILD_STEPS_PARAM}|g" \
        -e "s|\${REPO_URL_PARAM}|${esc_repo}|g" \
        "$TEMPLATE_FILE" > "$out"

    info "Jenkinsfile generated at: $out"
}

# --- Execution flow ---

autodetect_config
apply_env_overrides
validate_required
select_template "$TEMPLATE_NAME"
generate
