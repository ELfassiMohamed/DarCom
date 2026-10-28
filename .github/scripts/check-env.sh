#!/usr/bin/env bash
# Fails if:
#  1. a real .env file is tracked by git
#  2. code reads an env var that is missing from .env.example
set -uo pipefail
fail=0

# 1. .env must never be committed
if git ls-files --error-unmatch .env >/dev/null 2>&1; then
  echo "::error::.env is committed to git. Remove it (git rm --cached .env) and rotate any secrets inside."
  fail=1
fi

# 2. Compare env vars used in code vs .env.example
if [ ! -f .env.example ]; then
  echo "No .env.example found, skipping var comparison."
  exit $fail
fi

used=$(grep -rhoE \
  "process\.env\.[A-Z][A-Z0-9_]*|import\.meta\.env\.[A-Z][A-Z0-9_]*|os\.(environ(\.get)?|getenv)[\(\[]['\"][A-Z][A-Z0-9_]*" \
  --include=*.js --include=*.jsx --include=*.ts --include=*.tsx --include=*.mjs --include=*.py \
  --exclude-dir=node_modules --exclude-dir=.git --exclude-dir=dist --exclude-dir=build . \
  | grep -oE "[A-Z][A-Z0-9_]*$" | sort -u \
  | grep -vxE "NODE_ENV|CI|MODE|DEV|PROD|BASE_URL" || true)

documented=$(sed -nE 's/^[[:space:]]*(export[[:space:]]+)?([A-Z][A-Z0-9_]*)=.*/\2/p' .env.example | sort -u)

missing=$(comm -23 <(echo "$used") <(echo "$documented") | grep -v '^$' || true)

if [ -n "$missing" ]; then
  echo "::error::Env vars used in code but missing from .env.example:"
  echo "$missing"
  fail=1
else
  echo "Env check passed."
fi

exit $fail
