package com.intellij.internal.statistic.eventLog

import com.intellij.internal.statistic.eventLog.validator.ValidationResultType
import com.jetbrains.fus.reporting.model.lion3.LogEvent
import com.jetbrains.fus.reporting.model.lion3.LogEventAction
import com.jetbrains.fus.reporting.model.lion3.LogEventGroup
import com.jetbrains.fus.reporting.model.lion4.FusRecorder
import java.lang.StringBuilder
import java.util.*
import javax.naming.ldap.Rdn.escapeValue
import kotlin.collections.HashMap

object StatisticsEventEscaper {
    private const val SYMBOLS_TO_REPLACE = ":;, "
    private const val SYMBOLS_TO_REPLACE_FIELD_NAME = '.'.toString() + SYMBOLS_TO_REPLACE

    /**
     * Only printable ASCII symbols except whitespaces and '" are allowed.
     */
    @JvmStatic
    fun escapeEventIdOrFieldValue(str: String): String {
        return escapeInternal(str, null, true)
    }

    /**
     * Only printable ASCII symbols except whitespaces and :;,'" are allowed.
     */
    @JvmStatic
    fun escape(str: String): String {
        return escapeInternal(str, SYMBOLS_TO_REPLACE, false)
    }

    /**
     * Only printable ASCII symbols except whitespaces and .:;,'" are allowed.
     */
    @JvmStatic
    fun escapeFieldName(str: String): String {
        return if (Arrays.stream(ValidationResultType.values())
                        .anyMatch { value: ValidationResultType -> str == value.description }
        ) str else escapeInternal(str, SYMBOLS_TO_REPLACE_FIELD_NAME, false)
    }

    /**
     * Removes symbols prohibited in 2020.2 or earlier versions but allowed in 2020.3+.
     * Used for backward compatibility with validation rules create before 2020.2.
     *
     * @return null if there are no prohibited symbols
     */
    @JvmStatic
    fun cleanupForLegacyRulesIfNeeded(str: String): String? {
        return if (containsSystemSymbols(
                        str,
                        SYMBOLS_TO_REPLACE
                )
        ) {
            replace(
                    str,
                    SYMBOLS_TO_REPLACE,
                    false
            )
        } else null
    }

    @JvmStatic
    fun escapeEventData(eventData: Map<String, Any>): HashMap<String, Any> {
        val escapedData = hashMapOf<String, Any>()
        for ((key, value) in eventData) {
            escapedData[escapeFieldName(key)] = escapeEventDataValue(value)
        }
        return escapedData
    }

    @JvmStatic
    fun escapeIds(eventData: Map<String, String>): HashMap<String, String> {
        val escapedData = hashMapOf<String, String>()
        for ((key, value) in eventData) {
            escapedData[escapeFieldName(key)] = escapeEventIdOrFieldValue(value)
        }
        return escapedData
    }

    @JvmStatic
    fun escapeEventDataValue(value: Any): Any {
        return when (value) {
            is String -> escapeEventIdOrFieldValue(value)
            is List<*> -> value.map { if (it != null) escapeEventDataValue(it) else it }
            is Map<*, *> -> {
                value.entries.associate { (entryKey, entryValue) ->
                    val newKey = if (entryKey is String) escapeFieldName(entryKey) else entryKey
                    val newValue = if (entryValue != null) escapeEventDataValue(entryValue) else entryValue
                    newKey to newValue
                }
            }
            else -> value
        }
    }

    private fun escapeInternal(str: String, toReplace: String?, allowSpaces: Boolean): String {
        return if (containsSystemSymbols(str, toReplace)) {
            replace(str, toReplace, allowSpaces)
        } else str
    }

    private fun replace(value: String, toReplace: String?, allowSpaces: Boolean): String {
        val out = StringBuilder()
        for (i in 0 until value.length) {
            val c = value[i]
            if (!isAscii(c)) {
                out.append("?")
            } else if (isWhiteSpaceToReplace(c)) {
                out.append(if (allowSpaces) " " else "_")
            } else if (isSymbolToReplace(c, toReplace)) {
                out.append("_")
            } else if (!isProhibitedSymbol(c)) {
                out.append(c)
            }
        }
        return out.toString()
    }

    private fun containsSystemSymbols(value: String, toReplace: String?): Boolean {
        for (i in 0 until value.length) {
            val c = value[i]
            if (!isAscii(c)) return true
            if (isWhiteSpaceToReplace(c)) return true
            if (isSymbolToReplace(c, toReplace)) return true
            if (isProhibitedSymbol(c)) return true
        }
        return false
    }

    private fun isAscii(c: Char): Boolean {
        return c.toInt() <= 127
    }

    private fun isSymbolToReplace(c: Char, toReplace: String?): Boolean {
        return if (toReplace != null && containsChar(toReplace, c)) {
            true
        } else isAsciiControl(c)
    }

    fun isWhiteSpaceToReplace(c: Char): Boolean {
        return c == '\n' || c == '\r' || c == '\t'
    }

    private fun isAsciiControl(c: Char): Boolean {
        return c.toInt() < 32 || c.toInt() == 127
    }

    private fun isProhibitedSymbol(c: Char): Boolean {
        return c == '\'' || c == '"'
    }

    private fun containsChar(str: String, c: Char): Boolean {
        for (i in 0 until str.length) {
            if (str[i] == c) return true
        }
        return false
    }
}

/**
 * Creates LogEvent with all fields escaped
 */
fun newLogEvent(
        session: String,
        build: String,
        bucket: String,
        time: Long,
        groupId: String,
        groupVersion: String,
        recorderVersion: String,
        eventId: String,
        isState: Boolean = false,
        eventData: Map<String, Any> = emptyMap(),
        count: Int = 1
): LogEvent {
    val escapedData = StatisticsEventEscaper.escapeEventData(eventData)
    val event = LogEventAction(StatisticsEventEscaper.escapeEventIdOrFieldValue(eventId), isState, escapedData, count)
    val group = LogEventGroup(StatisticsEventEscaper.escape(groupId), StatisticsEventEscaper.escape(groupVersion))
    return LogEvent(
            StatisticsEventEscaper.escape(session),
            StatisticsEventEscaper.escape(build),
            StatisticsEventEscaper.escape(bucket),
            time,
            group,
            StatisticsEventEscaper.escape(recorderVersion),
            event
    )
}

fun LogEvent.escape(): LogEvent {
    val escapedData = StatisticsEventEscaper.escapeEventData(event.data)
    val event = LogEventAction(
            StatisticsEventEscaper.escapeEventIdOrFieldValue(event.id),
            event.state,
            escapedData,
            event.count
    )
    val group = LogEventGroup(StatisticsEventEscaper.escape(group.id), StatisticsEventEscaper.escape(group.version))
    return LogEvent(
            StatisticsEventEscaper.escape(session),
            StatisticsEventEscaper.escape(build),
            StatisticsEventEscaper.escape(bucket),
            time,
            group,
            StatisticsEventEscaper.escape(recorderVersion),
            event
    )
}

fun LogEventAction.addEscapedData(key: String, value: Any) {
    data[StatisticsEventEscaper.escapeFieldName(key)] = escapeValue(value)
}

fun com.jetbrains.fus.reporting.model.lion4.LogEvent.escape(): com.jetbrains.fus.reporting.model.lion4.LogEvent {
    val escapedIds = StatisticsEventEscaper.escapeIds(ids)
    val escapedGroup = com.jetbrains.fus.reporting.model.lion4.LogEventGroup(
            StatisticsEventEscaper.escape(group.id),
            group.version,
            group.state
    )
    val escapedData = StatisticsEventEscaper.escapeEventData(event.data)
    val escapedEvent = com.jetbrains.fus.reporting.model.lion4.LogEventAction(
            StatisticsEventEscaper.escapeEventIdOrFieldValue(event.id),
            escapedData,
            event.count
    )
    val eventSession = session
    val escapedSession = if (eventSession == null) null else StatisticsEventEscaper.escape(eventSession)
    return com.jetbrains.fus.reporting.model.lion4.LogEvent(
            FusRecorder(StatisticsEventEscaper.escape(recorder.id), recorder.version),
            StatisticsEventEscaper.escape(product),
            escapedIds,
            internal,
            time,
            StatisticsEventEscaper.escape(build),
            escapedSession,
            escapedGroup,
            bucket,
            escapedEvent,
            system_data
    )
}
