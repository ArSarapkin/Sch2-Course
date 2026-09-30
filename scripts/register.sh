#!/usr/bin/env bash
#
# Регистрирует студента через admin и печатает его mine_token и city_token.
#
#   ./scripts/register.sh <login> <name> <lastname>
#
# Переменные окружения:
#   ADMIN_URL   адрес admin, по умолчанию http://51.250.102.79:8083
#   ADMIN_KEY   ключ администратора; если не задан, берётся из .env в корне проекта

set -euo pipefail

if [[ $# -ne 3 ]]; then
    echo "Использование: $0 <login> <name> <lastname>" >&2
    exit 1
fi

ADMIN_URL="${ADMIN_URL:-http://51.250.102.79:8083}"

if [[ -z "${ADMIN_KEY:-}" ]]; then
    env_file="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env"
    ADMIN_KEY="$(grep -m1 '^ADMIN_KEY=' "$env_file" 2>/dev/null | cut -d= -f2- || true)"
fi
if [[ -z "$ADMIN_KEY" ]]; then
    echo "ADMIN_KEY не задан ни в окружении, ни в .env" >&2
    exit 1
fi

# экранирование для JSON-строки: обратный слэш и кавычка
json_str() {
    local s="${1//\\/\\\\}"
    printf '"%s"' "${s//\"/\\\"}"
}

body="{\"login\":$(json_str "$1"),\"name\":$(json_str "$2"),\"lastname\":$(json_str "$3")}"

# ключ передаётся curl через stdin, чтобы не светиться в списке процессов
printf 'header = "Authorization: Bearer %s"\n' "$ADMIN_KEY" |
    curl --silent --show-error --fail-with-body --config - \
        --header 'Content-Type: application/json' \
        --data "$body" \
        "$ADMIN_URL/register"
echo
