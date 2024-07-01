#!/bin/bash
set -e

CONFIG_FILE="configs/sample_config.env"
TEMPLATE_FILE="templates/Jenkinsfile.template"
OUTPUT_FILE="output/Jenkinsfile"

[[ ! -f "$CONFIG_FILE" ]] && { echo "[Error]: Config file not found"; exit 1; }
[[ ! -f "$TEMPLATE_FILE" ]] && { echo "[Error]: Template file not found"; exit 1; }

# Load vars
source "$CONFIG_FILE"

REQUIRED_VARS=("BRANCH_NAME_PARAM" "BUILD_TOOL_PARAM" "BUILD_STEPS_PARAM")
for var in "${REQUIRED_VARS[@]}"; do
  [[ -z "${!var}" ]] && { echo "[Error]: Missing variable: $var"; exit 1; }
done

mkdir -p output

# Replace placeholders
sed -e "s|\${BRANCH_NAME_PARAM}|$BRANCH_NAME_PARAM|g" \
    -e "s|\${BUILD_TOOL_PARAM}|$BUILD_TOOL_PARAM|g" \
    -e "s|\${BUILD_STEPS_PARAM}|$BUILD_STEPS_PARAM|g" \
    "$TEMPLATE_FILE" > "$OUTPUT_FILE"

echo "Jenkinsfile generated at $OUTPUT_FILE"
