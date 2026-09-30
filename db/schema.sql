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

-- Успешные dig в сервисе mine; неудачные попытки (429) сюда не пишутся
CREATE TABLE IF NOT EXISTS dig_log
(
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    time     TIMESTAMPTZ NOT NULL,
    login    VARCHAR(64) NOT NULL REFERENCES student (login) ON DELETE CASCADE,
    resourse UUID        NOT NULL UNIQUE
);

-- для загрузки времени последнего dig каждого студента при старте mine
CREATE INDEX IF NOT EXISTS dig_log_login_time_idx ON dig_log (login, time);
