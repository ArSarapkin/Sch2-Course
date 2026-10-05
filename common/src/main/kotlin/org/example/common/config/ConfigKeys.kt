package org.example.common.config

/**
 * Все допустимые ключи таблицы `config`.
 */
object ConfigKeys {

    /** Минимальный интервал между успешными dig одного студента в mine, мс. */
    const val DIG_TIMEOUT_MS = "DIG_TIMEOUT_MS"

    /** Список всех ключей; новый ключ нужно добавить и сюда, иначе админ-панель не даст его задать. */
    val ALL: List<String> = listOf(
        DIG_TIMEOUT_MS,
    )
}
