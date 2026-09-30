#!/usr/bin/env python3
"""
Бот для проверки mine и city: каждые 500 мс делает dig, сразу продаёт добытое и печатает баланс.

    ./scripts/bot.py <mine_token> <city_token>
    MINE_TOKEN=… CITY_TOKEN=… ./scripts/bot.py

Токены выдаёт ./scripts/register.sh. Адрес сервера — переменная HOST (по умолчанию 51.250.102.79).
Остановка — Ctrl+C, после неё печатается итог.
"""

import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

HOST = os.environ.get("HOST", "51.250.102.79")
MINE_URL = f"http://{HOST}:8081"
CITY_URL = f"http://{HOST}:8082"
INTERVAL_S = 0.5
TIMEOUT_S = 5


def request(method, url, token):
    """(статус, заголовки, тело как dict или None); статус 0 — сервер недоступен."""
    req = urllib.request.Request(url, method=method, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT_S) as resp:
            return resp.status, resp.headers, parse(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, e.headers, parse(e.read())
    except (urllib.error.URLError, OSError) as e:
        return 0, {}, {"error": str(e)}


def parse(raw):
    try:
        return json.loads(raw) if raw else None
    except ValueError:
        return None


def log(message):
    print(f"{time.strftime('%H:%M:%S')} {message}", flush=True)


def main():
    if len(sys.argv) == 3:
        mine_token, city_token = sys.argv[1], sys.argv[2]
    else:
        mine_token, city_token = os.environ.get("MINE_TOKEN"), os.environ.get("CITY_TOKEN")
    if not mine_token or not city_token:
        sys.exit(__doc__)

    stats = {"dig": 0, "too_early": 0, "sold": 0, "errors": 0}

    status, _, body = request("GET", f"{CITY_URL}/get_balance", city_token)
    if status != 200:
        sys.exit(f"get_balance: {status} {body}")
    start_balance = balance = body["money"]
    log(f"старт, баланс {balance}, сервер {HOST}")

    next_tick = time.monotonic()
    try:
        while True:
            status, headers, body = request("POST", f"{MINE_URL}/dig", mine_token)
            if status == 200:
                stats["dig"] += 1
                resource = body["resource"]
                status, _, sold = request("POST", f"{CITY_URL}/sell?resource={urllib.parse.quote(resource)}", city_token)
                if status == 200 and sold["sold"]:
                    stats["sold"] += 1
                    # sell не сообщает цену, поэтому выручку видно только по изменению баланса
                    b_status, _, b_body = request("GET", f"{CITY_URL}/get_balance", city_token)
                    if b_status == 200:
                        log(f"dig → {resource} продан, баланс {b_body['money']} (+{b_body['money'] - balance})")
                        balance = b_body["money"]
                    else:
                        stats["errors"] += 1
                        log(f"dig → {resource} продан, get_balance: {b_status} {b_body}")
                else:
                    stats["errors"] += 1
                    log(f"dig → {resource}, sell: {status} {sold}")
            elif status == 429:
                stats["too_early"] += 1
                reason = body.get("reason") if isinstance(body, dict) else None
                log(f"dig: 429 {reason}, ждать ещё {headers.get('Retry-After-Ms')} мс")
            else:
                stats["errors"] += 1
                log(f"dig: {status} {body}")

            # ровный шаг 500 мс независимо от времени ответа; после долгого ответа не догоняем пачкой
            next_tick = max(next_tick + INTERVAL_S, time.monotonic())
            time.sleep(max(0.0, next_tick - time.monotonic()))
    except KeyboardInterrupt:
        pass

    status, _, body = request("GET", f"{CITY_URL}/get_balance", city_token)
    if status == 200:
        balance_info = f"баланс {body['money']} (+{body['money'] - start_balance} за запуск)"
    else:
        balance_info = f"баланс не получен ({status})"
    print(
        f"\nИтог: dig {stats['dig']}, 429 {stats['too_early']}, продано {stats['sold']}, "
        f"ошибок {stats['errors']}, {balance_info}"
    )


if __name__ == "__main__":
    main()
