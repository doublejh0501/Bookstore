#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ -f .env ]]; then
  echo "==> Loading .env into environment"
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
else
  echo "WARN: .env file not found at project root; proceeding without it." >&2
fi

./gradlew bootRun

