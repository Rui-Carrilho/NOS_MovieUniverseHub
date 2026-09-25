#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
read -r -p "Existing profile (e.g. ana): " HUB_ACCOUNT_USERNAME
read -r -s -p "New password (10+ characters, max 72 UTF-8 bytes): " HUB_ACCOUNT_PASSWORD
printf '\n'
export HUB_ACCOUNT_USERNAME HUB_ACCOUNT_PASSWORD
trap 'unset HUB_ACCOUNT_USERNAME HUB_ACCOUNT_PASSWORD' EXIT
docker compose run --rm --no-deps -e HUB_ACCOUNT_USERNAME -e HUB_ACCOUNT_PASSWORD app \
  --spring.main.web-application-type=none --app.seed.enabled=false --app.account.enabled=true
