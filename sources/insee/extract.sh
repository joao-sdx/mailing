#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR"

if ! python3 -c "import yaml" 2>/dev/null; then
  echo "Installation de pyyaml..."
  pip3 install pyyaml -q
fi

python3 extract_credit_bail.py