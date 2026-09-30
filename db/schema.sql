-- Общая схема БД для сервисов mine, city и admin.

CREATE TABLE IF NOT EXISTS student
(
    login           VARCHAR(64)  PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    lastname        VARCHAR(128) NOT NULL,
    -- SHA-256 от токена в hex; сами токены в БД не хранятся
    mine_token_hash CHAR(64)     NOT NULL UNIQUE,
    city_token_hash CHAR(64)     NOT NULL UNIQUE
);

-- Динамическая конфигурация логики mine и city для конкретного студента
CREATE TABLE IF NOT EXISTS config
(
    login VARCHAR(64)  NOT NULL REFERENCES student (login) ON DELETE CASCADE,
    key   VARCHAR(128) NOT NULL,
    value TEXT         NOT NULL,
    PRIMARY KEY (login, key)
);

-- Деньги студента в city, целое число
CREATE TABLE IF NOT EXISTS balance
(
    login VARCHAR(64) PRIMARY KEY REFERENCES student (login) ON DELETE CASCADE,
    money BIGINT      NOT NULL DEFAULT 0 CHECK (money >= 0)
);

-- Ресурсы, добытые через dig в mine; при продаже в city запись удаляется
CREATE TABLE IF NOT EXISTS resource
(
    uuid UUID        PRIMARY KEY,
    type VARCHAR(32) NOT NULL
);

-- Успешные dig в сервисе mine; неудачные попытки (429) сюда не пишутся
CREATE TABLE IF NOT EXISTS dig_log
(
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    time     TIMESTAMPTZ NOT NULL,
    login    VARCHAR(64) NOT NULL REFERENCES student (login) ON DELETE CASCADE,
    -- без внешнего ключа на resource: при продаже ресурс удаляется, а лог остаётся
    resourse UUID        NOT NULL UNIQUE
);

-- для загрузки времени последнего dig каждого студента при старте mine
CREATE INDEX IF NOT EXISTS dig_log_login_time_idx ON dig_log (login, time);
