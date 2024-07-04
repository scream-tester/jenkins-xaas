#!/usr/bin/env bash
#
# Supported flags:
#   -c, --config FILE          : Config file (.env or .yaml)
#   -t, --template NAME        : Template pack (folder name under templates/)
#   -o, --output FILE          : Output Jenkinsfile path (default: output/Jenkinsfile)
#       --set KEY=VALUE        : Override a variable (repeatable). Accepts:
#                                BRANCH_NAME, BUILD_TOOL, BUILD_STEPS, REPO_URL
#   -l, --list-templates       : List available template packs and exit
#   -v, --version              : Print version and exit
#   -h, --help                 : Print usage and exit
#
# Examples:
#   ./generator.sh -c configs/sample_config.yaml -t basic
#   ./generator.sh --template docker-node --set BRANCH_NAME=main --set REPO_URL=https://...
#   ./generator.sh -o ./dist/Jenkinsfile --config configs/sample_config.env

set -euo pipefail

# --- Constants / Defaults ---
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
LIB_DIR="$ROOT_DIR/lib"
TEMPLATES_DIR="$ROOT_DIR/templates"
OUTPUT_DIR="$ROOT_DIR/output"
CONFIGS_DIR="$ROOT_DIR/configs"
VERSION_FILE="$ROOT_DIR/VERSION"

DEFAULT_ENV_CONFIG="$CONFIGS_DIR/sample_config.env"
DEFAULT_YAML_CONFIG="$CONFIGS_DIR/sample_config.yaml"
DEFAULT_TEMPLATE_PACK="basic"
DEFAULT_TEMPLATE_FILE="Jenkinsfile.tmpl"
DEFAULT_OUTPUT_FILE="$OUTPUT_DIR/Jenkinsfile"

# Runtime-configurable (CLI/env)
CONFIG_FILE="${CONFIG_FILE:-}"
TEMPLATE_NAME="${TEMPLATE_NAME:-$DEFAULT_TEMPLATE_PACK}"
OUTPUT_FILE="${OUTPUT_FILE:-$DEFAULT_OUTPUT_FILE}"

# --- Helpers ---
# shellcheck source=lib/common.sh
source "$LIB_DIR/common.sh"

require_file() { [[ -f "$1" ]] || die "File not found: $1"; }
require_dir()  { [[ -d "$1" ]] || die "Directory not found: $1"; }

version() {
    if [[ -f "$VERSION_FILE" ]]; then
        printf "pipeline-generator %s\n" "$(cat "$VERSION_FILE")"
    else
        printf "pipeline-generator (version unknown)\n"
    fi
}

usage() {
  cat <<'EOF'
Usage: generator.sh [options]

Options:
    -c, --config FILE          Config file (.env or .yaml)
    -t, --template NAME        Template pack name under templates/ (default: basic)
    -o, --output FILE          Output Jenkinsfile path (default: output/Jenkinsfile)
        --set KEY=VALUE        Override one variable (repeatable). Supported keys:
                                BRANCH_NAME, BUILD_TOOL, BUILD_STEPS, REPO_URL
    -l, --list-templates       List available template packs and exit
    -v, --version              Print version and exit
    -h, --help                 Show this help and exit

Examples:
    ./generator.sh -c configs/sample_config.yaml -t basic
    ./generator.sh --template docker-node --set BRANCH_NAME=release/1.2.3
    ./generator.sh -o ./dist/Jenkinsfile --config configs/sample_config.env

Notes:
- .env/.yaml set *_PARAM variables used in templates (e.g., BRANCH_NAME_PARAM).
- --set BRANCH_NAME=main overrides via environment (mapped to *_PARAM internally).
EOF
}

# --- Loader functions ---

load_env_config() {
    local file="$1"
    info "Loading .env config: $file"
    # shellcheck disable=SC1090
    source "$file"
}

yaml_get_scalar() {
    # Extract a top-level YAML key's scalar value, preserving ':' in values
    # Works with leading spaces, tabs, CRLF, and quoted values
    local key="$1" file="$2"

    awk -v k="$key" '
        {
            # Remove CR in case of CRLF
            sub(/\r$/, "")

            # Match optional leading spaces before key
            if ($0 ~ "^[ \t]*" k ":") {
                # Remove everything up to and including the first colon
                sub(/^[ \t]*[^:]+:[ \t]*/, "", $0)

                # Trim leading/trailing spaces
                gsub(/^[ \t]+|[ \t]+$/, "", $0)

                # Remove surrounding quotes if present
                if ($0 ~ /^".*"$/ || $0 ~ /^'\''.*'\''$/) {
                    sub(/^["'\''"]/, "", $0)
                    sub(/["'\''"]$/, "", $0)
                }

                print $0
                exit
            }
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
            *.env)         load_env_config "$CONFIG_FILE" ;;
            *.yml|*.yaml)  load_yaml_config "$CONFIG_FILE" ;;
            *)             die "Unsupported config format (use .env or .yaml): $CONFIG_FILE" ;;
        esac
        return
    fi

    # Fallback to defaults
    if [[ -f "$DEFAULT_ENV_CONFIG" ]]; then
        load_env_config "$DEFAULT_ENV_CONFIG"
    elif [[ -f "$DEFAULT_YAML_CONFIG" ]]; then
        load_yaml_config "$DEFAULT_YAML_CONFIG"
    else
        die "No default config found. Provide --config or create $DEFAULT_ENV_CONFIG / $DEFAULT_YAML_CONFIG"
    fi
}

apply_env_overrides() {
    # Allow environment variables to override config file values
    # (useful for Jenkins parameters and --set KEY=VALUE on CLI)
    if [[ -n "${BRANCH_NAME:-}"   ]]; then BRANCH_NAME_PARAM="$BRANCH_NAME";   fi
    if [[ -n "${BUILD_TOOL:-}"    ]]; then BUILD_TOOL_PARAM="$BUILD_TOOL";     fi
    if [[ -n "${BUILD_STEPS:-}"   ]]; then BUILD_STEPS_PARAM="$BUILD_STEPS";   fi
    if [[ -n "${REPO_URL:-}"      ]]; then REPO_URL_PARAM="$REPO_URL";         fi
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

list_templates() {
   # Portable listing of immediate subdirs
    local any=0

    # Capture templates into an array
    local templates=()
    for d in "$TEMPLATES_DIR"/*/ ; do
        [[ -d "$d" ]] || continue
        any=1
        templates+=("$(basename "$d")")
    done

    # If none found
    if [[ $any -eq 0 ]]; then
        info "No template packs found in $TEMPLATES_DIR"
        return
    fi

    # Sort and print
    printf "%s\n" "${templates[@]}" | sort
}

generate() {
    mkdir -p "$(dirname "$OUTPUT_FILE")"

    # Escape values for safe sed replacement
    local esc_branch esc_tool esc_steps esc_repo
    esc_branch="$(escape_for_sed "${BRANCH_NAME_PARAM}")"
    esc_tool="$(escape_for_sed   "${BUILD_TOOL_PARAM}")"
    esc_steps="$(escape_for_sed  "${BUILD_STEPS_PARAM}")"
    esc_repo="$(escape_for_sed   "${REPO_URL_PARAM}")"

    sed -e "s|\${BRANCH_NAME_PARAM}|${esc_branch}|g" \
        -e "s|\${BUILD_TOOL_PARAM}|${esc_tool}|g" \
        -e "s|\${BUILD_STEPS_PARAM}|${esc_steps}|g" \
        -e "s|\${REPO_URL_PARAM}|${esc_repo}|g" \
        "$TEMPLATE_FILE" > "$OUTPUT_FILE"

    info "Jenkinsfile generated at: $OUTPUT_FILE"
}

# --- CLI parsing ---

# We parse long & short options manually for portability
if [[ $# -eq 0 ]]; then
    # No args is valid (auto-detect config/template), but show hint
    info "No CLI options supplied; using defaults (auto-detect config, template: $TEMPLATE_NAME)."
fi

while [[ $# -gt 0 ]]; do
    case "$1" in
        -c|--config)
            [[ $# -ge 2 ]] || die "Missing value for $1"
            CONFIG_FILE="$2"; shift 2 ;;
        -t|--template)
            [[ $# -ge 2 ]] || die "Missing value for $1"
            TEMPLATE_NAME="$2"; shift 2 ;;
        -o|--output)
            [[ $# -ge 2 ]] || die "Missing value for $1"
            OUTPUT_FILE="$2"; shift 2 ;;
        --set)
            [[ $# -ge 2 ]] || die "Missing value for --set (expected KEY=VALUE)"
            case "$2" in
                BRANCH_NAME=*|BUILD_TOOL=*|BUILD_STEPS=*|REPO_URL=*)
                    # Export env override for apply_env_overrides()
                    export "${2%%=*}"="${2#*=}"
                    shift 2
                    ;;
                *)
                    die "Unsupported --set key. Use one of: BRANCH_NAME, BUILD_TOOL, BUILD_STEPS, REPO_URL"
                    ;;
            esac
            ;;
        -l|--list-templates)
            list_templates
            exit 0 ;;
        -v|--version)
            version
            exit 0 ;;
        -h|--help)
            usage
            exit 0 ;;
        --) shift; break ;;   # end of options
        -*)
            die "Unknown option: $1 (use --help)"
            ;;
        *)
            die "Unexpected argument: $1 (use --help)"
            ;;
    esac
done

# --- Execution flow ---
autodetect_config
apply_env_overrides
validate_required
select_template "$TEMPLATE_NAME"
generate
