#!/usr/bin/env bash
#
# Собирает mine, city и admin из текущего кода и разворачивает их в Docker на сервере,
# сворачивая уже запущенные там версии.
#
#   ./deploy/deploy.sh
#
# Переменные окружения:
#   DEPLOY_HOST        адрес сервера для ssh (user@host или алиас из ~/.ssh/config), по умолчанию 51.250.102.79
#   DEPLOY_DIR         папка проекта на сервере, по умолчанию /opt/sch2
#   DEPLOY_SERVICES    какие сервисы разворачивать, по умолчанию "mine city admin"
#
# На сервере нужен Docker (без sudo). Локальный .env из корня проекта копируется в $DEPLOY_DIR/.env
# и заменяет прежний. Контейнеры запускаются в сети хоста, поэтому DB_HOST в .env должен указывать
# на базу так, как она видна с самого сервера.

set -euo pipefail

DEPLOY_HOST="${DEPLOY_HOST:-51.250.102.79}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/sch2}"
DEPLOY_SERVICES="${DEPLOY_SERVICES:-mine city admin}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() { printf '\n==> %s\n' "$*"; }

if [[ ! -f .env ]]; then
    echo "Нет $ROOT_DIR/.env: без него сервисам на сервере не хватит переменных DB_* и ADMIN_KEY" >&2
    exit 1
fi

# Версия: коммит, время сборки и пометка о незакоммиченных изменениях
VERSION="$(git rev-parse --short HEAD)-$(date +%Y%m%d%H%M%S)"
if [[ -n "$(git status --porcelain)" ]]; then
    VERSION="$VERSION-dirty"
    echo "Внимание: есть незакоммиченные изменения, они тоже попадут в сборку" >&2
fi

log "Сборка $VERSION: $DEPLOY_SERVICES"
tasks=()
for service in $DEPLOY_SERVICES; do
    tasks+=(":$service:bootJar")
done
# тесты требуют живую БД, поэтому собираем только jar
./gradlew --quiet "${tasks[@]}"

staging="$(mktemp -d)"

# Одно ssh-подключение на весь запуск: пароль ключа спрашивается один раз
# сокет в коротком пути: ssh ограничивает его длину 104 байтами, а $TMPDIR на macOS длинный
ssh_dir="$(mktemp -d /tmp/sch2-ssh.XXXXXX)"
SSH_OPTS=(-o ControlMaster=auto -o "ControlPath=$ssh_dir/cm" -o ControlPersist=120)
remote() { ssh "${SSH_OPTS[@]}" "$DEPLOY_HOST" "$@"; }
cleanup() {
    ssh "${SSH_OPTS[@]}" -O exit "$DEPLOY_HOST" 2>/dev/null || true
    rm -rf "$staging" "$ssh_dir"
}
trap cleanup EXIT
cp deploy/Dockerfile "$staging/Dockerfile"
for service in $DEPLOY_SERVICES; do
    mkdir -p "$staging/$service"
    cp "$service/build/libs/$service-1.0-SNAPSHOT.jar" "$staging/$service/app.jar"
done

log "Загрузка на $DEPLOY_HOST:$DEPLOY_DIR/releases/$VERSION"
# без xattr macOS, иначе tar на сервере ругается на неизвестные заголовки
COPYFILE_DISABLE=1 tar --no-xattrs -C "$staging" -czf - . | remote "mkdir -p '$DEPLOY_DIR/releases/$VERSION' && tar -xzf - -C '$DEPLOY_DIR/releases/$VERSION'"

log "Загрузка .env в $DEPLOY_HOST:$DEPLOY_DIR/.env"
# файл с паролями доступен на сервере только владельцу; chmod — на случай, если он уже был с другими правами
remote "umask 077 && cat > '$DEPLOY_DIR/.env' && chmod 600 '$DEPLOY_DIR/.env'" < .env

log "Разворачивание на сервере"
remote bash -s -- "$DEPLOY_DIR" "$VERSION" $DEPLOY_SERVICES <<'REMOTE'
set -euo pipefail

DEPLOY_DIR="$1"
VERSION="$2"
shift 2
SERVICES=("$@")

RELEASE_DIR="$DEPLOY_DIR/releases/$VERSION"
ENV_FILE="$DEPLOY_DIR/.env"

port_of() {
    case "$1" in
        mine) echo 8081 ;;
        city) echo 8082 ;;
        admin) echo 8083 ;;
        *) echo "Неизвестный сервис: $1" >&2; exit 1 ;;
    esac
}

if [[ ! -f "$ENV_FILE" ]]; then
    echo "Нет $ENV_FILE: он должен был загрузиться на предыдущем шаге" >&2
    exit 1
fi

# Сначала собираем все образы, чтобы при ошибке сборки старые версии остались работать
for service in "${SERVICES[@]}"; do
    echo "--- образ sch2-$service:$VERSION"
    cp "$RELEASE_DIR/Dockerfile" "$RELEASE_DIR/$service/Dockerfile"
    # при первом запуске здесь скачивается базовый образ, это может занять пару минут
    docker build --tag "sch2-$service:$VERSION" "$RELEASE_DIR/$service"
done
rm -rf "$RELEASE_DIR"

for service in "${SERVICES[@]}"; do
    port="$(port_of "$service")"

    # Сворачиваем все прежние контейнеры сервиса, включая остановленные
    old="$(docker ps -aq --filter "label=sch2.service=$service")"
    if [[ -n "$old" ]]; then
        echo "--- $service: сворачиваю старую версию"
        docker rm -f $old >/dev/null
    fi

    echo "--- $service: запуск $VERSION на порту $port"
    docker run -d \
        --name "sch2-$service" \
        --label "sch2.service=$service" \
        --label "sch2.version=$VERSION" \
        --network host \
        --env-file "$ENV_FILE" \
        --restart unless-stopped \
        "sch2-$service:$VERSION" >/dev/null

    # Ждём, пока сервис откроет порт; если контейнер упал — показываем логи
    for _ in $(seq 1 60); do
        if [[ "$(docker inspect -f '{{.State.Running}}' "sch2-$service")" != "true" ]]; then
            echo "!!! $service упал при старте:" >&2
            docker logs --tail 50 "sch2-$service" >&2
            exit 1
        fi
        if (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
            echo "--- $service: запущен"
            continue 2
        fi
        sleep 1
    done
    echo "!!! $service не открыл порт $port за 60 секунд:" >&2
    docker logs --tail 50 "sch2-$service" >&2
    exit 1
done

# Удаляем образы старых версий развёрнутых сервисов
for service in "${SERVICES[@]}"; do
    docker images "sch2-$service" --format '{{.Repository}}:{{.Tag}}' \
        | grep -v ":$VERSION\$" \
        | xargs -r docker rmi >/dev/null 2>&1 || true
done
REMOTE

log "Готово: $VERSION"
