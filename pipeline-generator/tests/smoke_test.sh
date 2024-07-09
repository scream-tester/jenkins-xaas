#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")"/.. && pwd)"
GEN="$ROOT/bin/pipeline-gen"

echo "== List templates =="
"$GEN" -l

echo
echo "== Validate good config (YAML) =="
"$GEN" -c "$ROOT/configs/sample_config.yaml" -t docker-node --validate-only

echo
echo "== Preview (stdout only) =="
# Should print a clean Jenkinsfile to stdout (no [Info] lines)
"$GEN" -c "$ROOT/configs/sample_config.yaml" -t docker-node --preview | head -n 8

echo
echo "== Generate to file =="
out="$ROOT/output/Jenkinsfile.smoke"
"$GEN" -c "$ROOT/configs/sample_config.yaml" -t basic -o "$out"
test -s "$out" && echo "OK: file generated at $out"

echo
echo "== Strict Validation invalid config (should fail) =="
if "$GEN" -c "$ROOT/configs/invalid_config.yaml" --strict; then
    echo "ERROR: invalid config unexpectedly passed!" >&2
    exit 1
else
    echo "OK: invalid config correctly failed validation."
fi

echo
echo "All smoke tests passed."
