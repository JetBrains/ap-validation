package com.intellij.internal.statistic.eventLog.validator

import com.intellij.internal.statistic.eventLog.EventLogBuild
import com.intellij.internal.statistic.eventLog.EventLogBuild.EVENT_LOG_BUILD_PRODUCER
import com.intellij.internal.statistic.eventLog.TestValidatorReturningImmutableData
import com.intellij.internal.statistic.eventLog.newLogEvent
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.beans.EventGroupContextData
import com.intellij.internal.statistic.eventLog.validator.rules.impl.UtilValidationRule
import com.intellij.internal.statistic.eventLog.validator.rules.utils.UtilRuleProducer
import com.jetbrains.fus.reporting.model.lion3.LogEvent
import com.jetbrains.fus.reporting.model.lion3.LogEventAction
import com.jetbrains.fus.reporting.model.lion3.LogEventGroup
import com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SensitiveDataValidatorTest : BaseSensitiveDataValidatorTest() {
    @Test
    fun `test validation fails with empty remote descriptors`() {
        val sensitiveDataValidator =
            SensitiveDataValidator(SimpleValidationRuleStorage(EventGroupRemoteDescriptors(), EVENT_LOG_BUILD_PRODUCER))
        val validatedEvent = sensitiveDataValidator.validateEvent(createEventLog(hashMapOf("count" to 42)))
        assertEquals(null, validatedEvent)
    }

    @Test
    fun `test replace unknown fields with error description`() {
        val groupDescriptors = createGroupDescriptors()
        val sensitiveDataValidator =
            SensitiveDataValidator(SimpleValidationRuleStorage(groupDescriptors, EVENT_LOG_BUILD_PRODUCER))
        val validatedEvent = sensitiveDataValidator.validateEvent(createEventLog(hashMapOf("count" to 42)))
        val data = validatedEvent?.event?.data
        assertNotNull(data)
        assertEquals(1, data.size)
        assertEquals(ValidationResultType.UNDEFINED_RULE.description,
            data[ValidationResultType.UNDEFINED_RULE.description])
    }

    @Test
    fun `test replace unknown fields with error description in nested objects`() {
        val groupDescriptors = createGroupDescriptors()
        val sensitiveDataValidator =
            SensitiveDataValidator(SimpleValidationRuleStorage(groupDescriptors, EVENT_LOG_BUILD_PRODUCER))
        val validatedEvent = sensitiveDataValidator.validateEvent(createEventLog(hashMapOf("count" to hashMapOf("foo" to 42))))
        val data = validatedEvent?.event?.data
        assertNotNull(data)
        assertEquals(1, data.size)
        assertEquals(hashMapOf(ValidationResultType.UNDEFINED_RULE.description to ValidationResultType.UNDEFINED_RULE.description),
            data[ValidationResultType.UNDEFINED_RULE.description])
    }


    @Test
    fun `test not replace known fields with error description in nested objects`() {
        val groupDescriptors = createGroupDescriptors(hashMapOf("count.foo" to setOf("{enum:foo|bar}")))
        val sensitiveDataValidator =
            SensitiveDataValidator(SimpleValidationRuleStorage(groupDescriptors, EVENT_LOG_BUILD_PRODUCER))
        val validatedEvent =
            sensitiveDataValidator.validateEvent(createEventLog(hashMapOf("count" to hashMapOf("foo" to "foo",
                "bar" to "bar"))))
        val data = validatedEvent?.event?.data
        assertNotNull(data)
        assertEquals(1, data.size)
        val count = data["count"] as? Map<*, *>
        assertNotNull(count)
        assertEquals("foo", count["foo"])
        assertEquals(ValidationResultType.UNDEFINED_RULE.description,
            count[ValidationResultType.UNDEFINED_RULE.description])
    }

    @Test
    fun `test replace unknown fields with error description in list of objects`() {
        val groupDescriptors = createGroupDescriptors()
        val sensitiveDataValidator =
            SensitiveDataValidator(SimpleValidationRuleStorage(groupDescriptors, EVENT_LOG_BUILD_PRODUCER))
        val eventData = hashMapOf("count" to listOf(hashMapOf("foo" to "foo"), hashMapOf("bar" to "bar")))
        val validatedEvent = sensitiveDataValidator.validateEvent(createEventLog(eventData))
        val data = validatedEvent?.event?.data
        assertNotNull(data)
        assertEquals(1, data.size)
        val count = data[ValidationResultType.UNDEFINED_RULE.description] as? List<*>
        assertNotNull(count)
        for (value in count) {
            assertEquals(hashMapOf(ValidationResultType.UNDEFINED_RULE.description to ValidationResultType.UNDEFINED_RULE.description),
                value)
        }
    }

    @Test
    fun `test not replace known fields with error description in list of objects`() {
        val groupDescriptors = createGroupDescriptors(hashMapOf("count.foo" to setOf("{enum:foo|bar}")))
        val sensitiveDataValidator =
            SensitiveDataValidator(SimpleValidationRuleStorage(groupDescriptors, EVENT_LOG_BUILD_PRODUCER))
        val eventData = hashMapOf("count" to listOf(hashMapOf("foo" to "foo"), hashMapOf("bar" to "bar")))
        val validatedEvent = sensitiveDataValidator.validateEvent(createEventLog(eventData))
        val data = validatedEvent?.event?.data
        assertNotNull(data)
        assertEquals(1, data.size)
        val count = data["count"] as? List<*>
        assertNotNull(count)
        assertEquals(hashMapOf("foo" to "foo"), count[0])
        assertEquals(hashMapOf(ValidationResultType.UNDEFINED_RULE.description to ValidationResultType.UNDEFINED_RULE.description),
            count[1])
    }

    @Test
    fun `test replace unknown fields with error description in list`() {
        val groupDescriptors = createGroupDescriptors()
        val sensitiveDataValidator =
            SensitiveDataValidator(SimpleValidationRuleStorage(groupDescriptors, EVENT_LOG_BUILD_PRODUCER))
        val validatedEvent =
            sensitiveDataValidator.validateEvent(createEventLog(hashMapOf("count" to listOf("foo", "bar"))))
        val data = validatedEvent?.event?.data
        assertNotNull(data)
        assertEquals(1, data.size)
        assertEquals(listOf(ValidationResultType.UNDEFINED_RULE.description, ValidationResultType.UNDEFINED_RULE.description),
            data[ValidationResultType.UNDEFINED_RULE.description])
    }

    @Test
    fun `test data in LogEventAction is always mutable`() {
        val groupDescriptors = createGroupDescriptors()
        val sensitiveDataValidator =
            TestValidatorReturningImmutableData(SimpleValidationRuleStorage(groupDescriptors, EVENT_LOG_BUILD_PRODUCER))
        val validatedEvent =
            sensitiveDataValidator.validateEvent(createEventLog(hashMapOf("count" to listOf("foo", "bar"))))
        val data = validatedEvent?.event?.data
        assertNotNull(data)
        data["key"] = "value"
    }

    @Test
    fun test_empty_rule() {
        val validator = newValidatorByFile("test_empty_rule.json")
        val eventLogGroup = LogEventGroup("build.gradle.actions", "1")

        val logEvent = createLogEvent(
            eventLogGroup,
            "<any-string-accepted>",
            data = hashMapOf("<any-string-accepted>" to "<any-string-accepted>")
        )
        val validatedEvent = validator.validateEvent(logEvent)
        assertNotNull(validatedEvent)
        assertEquals(ValidationResultType.UNDEFINED_RULE.description, validatedEvent.event.id)
        val data = validatedEvent.event.data.entries.firstOrNull()
        assertNotNull(data)
        assertEquals(ValidationResultType.UNDEFINED_RULE.description, data.key)
        assertEquals(ValidationResultType.UNDEFINED_RULE.description, data.value)
    }


    @Test
    fun test_simple_enum_rules() {
        val validator = newValidatorByFile("test_simple_enum_rules.json")
        var elg = LogEventGroup("my.simple.enum.value", "1")

        assertEventAccepted(validator, elg, "AAA")
        assertEventAccepted(validator, elg, "BBB")
        assertEventAccepted(validator, elg, "CCC")
        assertEventRejected(validator, elg, "ABC")

        elg = LogEventGroup("my.simple.enum.node.value", "1")
        assertEventAccepted(validator, elg, "NODE_AAA")
        assertEventAccepted(validator, elg, "NODE_BBB")
        assertEventAccepted(validator, elg, "NODE_CCC")
        assertEventRejected(validator, elg, "NODE_ABC")

        elg = LogEventGroup("my.simple.enum.ref", "1")
        assertEventAccepted(validator, elg, "REF_AAA")
        assertEventAccepted(validator, elg, "REF_BBB")
        assertEventAccepted(validator, elg, "REF_CCC")
        assertEventRejected(validator, elg, "REF_ABC")

        elg = LogEventGroup("my.simple.enum.node.ref", "1")
        assertEventAccepted(validator, elg, "NODE_REF_AAA")
        assertEventAccepted(validator, elg, "NODE_REF_BBB")
        assertEventAccepted(validator, elg, "NODE_REF_CCC")
        assertEventRejected(validator, elg, "NODE_REF_ABC")
    }

    @Test
    fun test_simple_enum_rules_with_spaces() {
        // enum values: ["NODE_REF_AAA", "NODE_REF_BBB", "NODE_REF_CCC"]
        val validator = newValidatorByFile("test_simple_enum_rules.json")

        val elg = LogEventGroup("my.simple.enum.node.ref", "1")
        assertEventAccepted(validator, elg, "NODE REF AAA")
        assertEventAccepted(validator, elg, "NOD'E;REF:BBB")
        assertEventAccepted(validator, elg, "NODE;REF:BBB")
        assertEventAccepted(validator, elg, "NODE_REF_BBB")
        assertEventAccepted(validator, elg, "NODE,REF,BBB")
        assertEventAccepted(validator, elg, "NOD'E_REF_BBB")
        assertEventAccepted(validator, elg, "NOD'E;REF_BBB")
        assertEventAccepted(validator, elg, "NOD'E:REF;BBB")
        assertEventAccepted(validator, elg, "NO\"DE REF CCC")

        assertEventRejected(validator, elg, "NODEREFCCC")
    }

    @Test
    fun test_simple_regexp_rules() {
        // custom regexp is:   (.+)\s*:\s*(.*)  => matches  'aaa/java.lang.String'
        val validator = newValidatorByFile("test_simple_regexp_rules.json")

        var elg = LogEventGroup("my.simple.regexp.value", "1")
        assertEventAccepted(validator, elg, "aaa/java.lang.String")
        assertEventRejected(validator, elg, "java.lang.String")

        elg = LogEventGroup("my.simple.regexp.node.value", "1")
        assertEventAccepted(validator, elg, "aaa/java.lang.String")
        assertEventRejected(validator, elg, "java.lang.String")

        elg = LogEventGroup("my.simple.regexp.ref", "1")
        assertEventAccepted(validator, elg, "aaa/java.lang.String")
        assertEventRejected(validator, elg, "java.lang.String")

        elg = LogEventGroup("my.simple.regexp.node.ref", "1")
        assertEventAccepted(validator, elg, "aaa/java.lang.String")
        assertEventRejected(validator, elg, "java.lang.String")

        elg = LogEventGroup("my.simple.regexp.with.number.of.elements", "1")
        assertEventAccepted(validator, elg, "0512345678ABCD023543")
        assertEventAccepted(validator, elg, "1154265567ABCD-23-43")
        assertEventAccepted(validator, elg, "0512345678QWER012-43")
        assertEventAccepted(validator, elg, "9965430987ASDF-01003")
        assertEventRejected(validator, elg, "aa65430987ASDF-01003")
        assertEventRejected(validator, elg, "999965430987ASDF-01003")
    }

    @Test
    fun test_global_integer_regex_rule() {
        val validator = newValidatorByFile("test_global_regexp_rules.json")

        val elg = LogEventGroup("regex.int.rule.group", "1")
        var value = 1000
        while (value > 0) {
            assertEventAccepted(validator, elg, value.toString())
            assertEventAccepted(validator, elg, (-1 * value).toString())
            value /= 2
        }
        assertEventAccepted(validator, elg, value.toString())
    }

    @Test
    fun test_global_double_regex_rule() {
        val validator = newValidatorByFile("test_global_regexp_rules.json")

        val elg = LogEventGroup("regex.double.rule.group", "1")
        var value = 100.0
        while (value > 0.00001) {
            assertEventAccepted(validator, elg, value.toString())
            assertEventAccepted(validator, elg, (-1 * value).toString())
            value /= 2
        }

        value = 1000000.0
        while (value < 100000000.0) {
            assertEventAccepted(validator, elg, value.toString())
            assertEventAccepted(validator, elg, (-1 * value).toString())
            value *= 2
        }
    }

    @Test
    fun test_simple_regexp_rules_with_spaces() {
        // custom regexp is:   [AB]_(.*) => matches  'A_x', 'A x'
        val validator = newValidatorByFile("test_simple_regexp_rules.json")

        val elg = LogEventGroup("my.simple.regexp.with.underscore", "1")
        assertEventAccepted(validator, elg, "A_x")
        assertEventAccepted(validator, elg, "A;x")
        assertEventAccepted(validator, elg, "A,x")
        assertEventAccepted(validator, elg, "A:x")
        assertEventAccepted(validator, elg, "A x")
        assertEventAccepted(validator, elg, "B:x")
        assertEventAccepted(validator, elg, "B;x")
        assertEventAccepted(validator, elg, "B_x")
        assertEventAccepted(validator, elg, "B x")
        assertEventAccepted(validator, elg, "B  x")
        assertEventAccepted(validator, elg, "B:_x")
        assertEventAccepted(validator, elg, "B:;x")
        assertEventAccepted(validator, elg, "B,,x")
        assertEventAccepted(validator, elg, "B__x")
        assertEventRejected(validator, elg, "Bxx")
    }

    @Test
    fun test_simple_expression_rules() {
        // custom expression is:   "JUST_TEXT[_{regexp:\\d+(\\+)?}_]_xxx_{enum:AAA|BBB|CCC}_zzz{enum#myEnum}_yyy"
        val validator = newValidatorByFile("test_simple_expression_rules.json")
        var elg = LogEventGroup("my.simple.expression", "1")

        assertEventAccepted(validator, elg, "JUST_TEXT[_123456_]_xxx_CCC_zzzREF_AAA_yyy")
        assertEventRejected(validator, elg, "JUST_TEXT[_FOO_]_xxx_CCC_zzzREF_AAA_yyy")
        assertEventRejected(validator, elg, "")

        //  {enum:AAA|}foo
        elg = LogEventGroup("my.simple.enum.node.with.empty.value", "1")
        assertEventAccepted(validator, elg, "AAAfoo")
        assertEventAccepted(validator, elg, "foo")
        assertEventRejected(validator, elg, " foo")
        assertEventRejected(validator, elg, " AAA foo")
    }

    @Test
    fun test_simple_expression_rules_with_spaces() {
        // custom expression is:   "JUST_TEXT[_{regexp:\\d+(\\+)?}_]_xxx_{enum:AAA|BBB|CCC}_zzz{enum#myEnum}_yyy"
        val validator = newValidatorByFile("test_simple_expression_rules.json")
        val elg = LogEventGroup("my.simple.expression", "1")

        assertEventAccepted(validator, elg, "JUST_TEXT[_123456_]:xxx_CCC_zzzREF_AAA_yyy")
        assertEventAccepted(validator, elg, "JUST_TEXT[_123456_]_xxx;CCC_zzzREF_AAA_yyy")
        assertEventAccepted(validator, elg, "JUST_TEXT[_123456_]:xxx,CCC_zzzREF_AAA_yyy")
        assertEventAccepted(validator, elg, "JUST_TEXT[_123456_]_xxx_CCC_zzzREF_AAA_yyy")
        assertEventAccepted(validator, elg, "JUST TEXT[_123456_]_xxx CCC,zzzREF:AAA;yyy")
        assertEventRejected(validator, elg, "JUSTTEXT[_123456_]_xxx!CCC_zzzREF:AAA;yyy")
    }

    @Test
    fun test_regexp_rule_with_global_regexps() {
        val validator = newValidatorByFile("test_regexp_rule-with-global-regexp.json")
        val elg = LogEventGroup("ui.fonts", "1")

        assertEventAccepted(validator, elg, "Presentation.mode.font.size[24]")
        assertEventAccepted(validator, elg, "IDE.editor.font.name[Monospaced]")
        assertEventAccepted(validator, elg, "IDE.editor.font.name[DejaVu_Sans_Mono]")
        assertEventAccepted(validator, elg, "Console.font.size[10]")

        assertEventRejected(validator, elg, "foo")
    }

    @Test
    fun test_validate_system_event_data() {
        val platformDataKeys: List<String> =
            listOf("plugin", "project", "os", "plugin_type", "lang", "current_file", "input_event", "place")
        val validator = newValidatorByFile(
            "test_validate_event_data.json",
            excludedFields = platformDataKeys
        )
        val elg = LogEventGroup("system.keys.group", "1")
        for (platformDataKey in platformDataKeys) {
            assertEventDataAccepted(validator, elg, platformDataKey, "<validated>")
        }
        assertEventDataAccepted(validator, elg, "ed_1", "AA")
        assertEventDataAccepted(validator, elg, "ed_2", "REF_BB")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.REJECTED, "ed_1", "CC")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.REJECTED, "ed_2", "REF_XX")
        assertEventDataNotAccepted(
            validator,
            elg,
            ValidationResultType.UNDEFINED_RULE,
            ValidationResultType.UNDEFINED_RULE.description,
            "<unknown>"
        )
    }

    @Test
    fun test_validate_escaped_event_data() {
        val validator = newValidatorByFile("test_validate_event_data.json")
        val elg = LogEventGroup("system.keys.group", "1")

        assertEventDataAccepted(validator, elg, "ed_1", "AA")
        assertEventDataAccepted(validator, elg, "ed_2", "REF_BB")
        assertEventDataNotAccepted(
            validator,
            elg,
            ValidationResultType.UNDEFINED_RULE,
            ValidationResultType.UNDEFINED_RULE.description,
            "REF_BB"
        )
    }

    @Test
    fun test_validate_custom_rule_with_local_enum() {
        val rule = TestLocalEnumCustomValidationRule()

        assertEquals(ValidationResultType.ACCEPTED, rule.validate("FIRST", EventContext.create("FIRST", emptyMap())))
        assertEquals(ValidationResultType.ACCEPTED, rule.validate("SECOND", EventContext.create("FIRST", emptyMap())))
        assertEquals(ValidationResultType.ACCEPTED, rule.validate("THIRD", EventContext.create("FIRST", emptyMap())))

        assertEquals(ValidationResultType.REJECTED, rule.validate("FORTH", EventContext.create("FIRST", emptyMap())))
        assertEquals(ValidationResultType.REJECTED, rule.validate("", EventContext.create("FIRST", emptyMap())))
        assertEquals(ValidationResultType.REJECTED, rule.validate("UNKNOWN", EventContext.create("FIRST", emptyMap())))
    }

    @Test
    fun test_joined_string_validation() {
        val validator = newValidatorByFile("test_join_string_regexp_rule.json")
        val group = LogEventGroup("my.test.rule.with.long.string", "1")

        val toAccept =
            arrayOf(
                "r50", "q50", "p", "p45", "p451", "k50", "l", "l50", "m11", "m40", "m403", "m5.0", "t", "t45", "t451",
                "g1.0", "g1.1", "g2.0", "g2.2", "g3.0", "g3.1", "f", "e1.0", "e1.3", "e1.6", "e2.0", "e2.1",
                "h-m5.0", "h-b12", "h-m40+d4", "h-m4.5+a8", "h-m4.5+y8.2", "h-m40+d5+a8", "h-a81+y81+w81",
                "h-m40+d5+a8+w81", "h-m40+d5+a8+w81+y8", "h-m40+a8+d4+y7", "h-m45+d5+a8+w81+y8",
                "d4", "d5", "c3", "c4", "b", "b10.0", "a", "a10", "a81", "x", "y", "y75", "y8", "w81"
            )

        for (value in toAccept) {
            assertEventAccepted(validator, group, value)
        }
        assertEventRejected(validator, group, "8")
        assertEventRejected(validator, group, "h-")
    }

    @Test
    fun test_validate_event_id_with_enum_and_existing_rule() {
        val validator = newValidatorByFile("test_rules_list_event_id.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("enum.and.existing.util.rule", "1")
        assertEventAccepted(validator, elg, "AAA")
        assertEventAccepted(validator, elg, "BBB")
        assertEventAccepted(validator, elg, "CCC")
        assertEventRejected(validator, elg, "DDD")
        assertEventAccepted(validator, elg, "FIRST")
    }

    @Test
    fun test_validate_event_id_with_enum_and_not_existing_rule() {
        val validator = newValidatorByFile("test_rules_list_event_id.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("enum.and.not.existing.util.rule", "1")
        assertEventAccepted(validator, elg, "AAA")
        assertEventAccepted(validator, elg, "BBB")
        assertEventAccepted(validator, elg, "CCC")
        assertIncorrectRule(validator, elg, "DDD")
        assertIncorrectRule(validator, elg, "FIRST")
    }

    @Test
    fun test_validate_event_id_with_enum_and_third_party_rule() {
        val validator = newValidatorByFile("test_rules_list_event_id.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("enum.and.third.party.util.rule", "1")
        assertEventAccepted(validator, elg, "AAA")
        assertEventAccepted(validator, elg, "BBB")
        assertEventAccepted(validator, elg, "CCC")
        assertThirdPartyRule(validator, elg, "DDD")
        assertEventAccepted(validator, elg, "FIRST")
        assertThirdPartyRule(validator, elg, "SECOND")
    }

    @Test
    fun test_validate_event_id_with_existing_rule_and_enum() {
        val validator = newValidatorByFile("test_rules_list_event_id.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("existing.util.rule.and.enum", "1")
        assertEventAccepted(validator, elg, "AAA")
        assertEventAccepted(validator, elg, "BBB")
        assertEventAccepted(validator, elg, "CCC")
        assertEventRejected(validator, elg, "DDD")
        assertEventAccepted(validator, elg, "FIRST")
    }

    @Test
    fun test_validate_event_id_with_not_existing_rule_and_enum() {
        val validator = newValidatorByFile("test_rules_list_event_id.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("not.existing.util.rule.and.enum", "1")
        assertEventAccepted(validator, elg, "AAA")
        assertEventAccepted(validator, elg, "BBB")
        assertEventAccepted(validator, elg, "CCC")
        assertEventRejected(validator, elg, "DDD")
        assertEventRejected(validator, elg, "FIRST")
    }

    @Test
    fun test_validate_event_id_with_third_party_rule_and_enum() {
        val validator = newValidatorByFile("test_rules_list_event_id.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("third.party.util.rule.and.enum", "1")
        assertEventAccepted(validator, elg, "AAA")
        assertEventAccepted(validator, elg, "BBB")
        assertEventAccepted(validator, elg, "CCC")
        assertEventRejected(validator, elg, "DDD")
        assertEventAccepted(validator, elg, "FIRST")
        assertEventRejected(validator, elg, "SECOND")
    }

    @Test
    fun test_validate_event_data_with_enum_and_existing_rule() {
        val validator = newValidatorByFile("test_rules_list_event_data.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("enum.and.existing.util.rule", "1")
        val data = hashMapOf<String, Any>()
        val dataRules = createLogEvent(elg, data = data)
        assertEventDataAccepted(validator, elg, "data_1", "AAA")
        assertEventDataAccepted(validator, elg, "data_1", "BBB")
        assertEventDataAccepted(validator, elg, "data_1", "CCC")
        assertEventDataAccepted(validator, elg, "data_1", "FIRST")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.REJECTED, "data_1", "DDD")
    }

    @Test
    fun test_validate_event_data_with_enum_and_not_existing_rule() {
        val validator = newValidatorByFile("test_rules_list_event_data.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("enum.and.not.existing.util.rule", "1")
        assertEventDataAccepted(validator, elg, "data_1", "AAA")
        assertEventDataAccepted(validator, elg, "data_1", "BBB")
        assertEventDataAccepted(validator, elg, "data_1", "CCC")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.INCORRECT_RULE, "data_1", "DDD")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.INCORRECT_RULE, "data_1", "FIRST")
    }

    @Test
    fun test_validate_event_data_with_enum_and_third_party_rule() {
        val validator = newValidatorByFile("test_rules_list_event_data.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("enum.and.third.party.util.rule", "1")
        assertEventDataAccepted(validator, elg, "data_1", "AAA")
        assertEventDataAccepted(validator, elg, "data_1", "BBB")
        assertEventDataAccepted(validator, elg, "data_1", "CCC")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.THIRD_PARTY, "data_1", "DDD")
        assertEventDataAccepted(validator, elg, "data_1", "FIRST")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.THIRD_PARTY, "data_1", "SECOND")
    }

    @Test
    fun test_validate_event_data_with_existing_rule_and_enum() {
        val validator = newValidatorByFile("test_rules_list_event_data.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("existing.util.rule.and.enum", "1")
        assertEventDataAccepted(validator, elg, "data_1", "AAA")
        assertEventDataAccepted(validator, elg, "data_1", "BBB")
        assertEventDataAccepted(validator, elg, "data_1", "CCC")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.REJECTED, "data_1", "DDD")
        assertEventDataAccepted(validator, elg, "data_1", "FIRST")
    }

    @Test
    fun test_validate_event_data_with_not_existing_rule_and_enum() {
        val validator = newValidatorByFile("test_rules_list_event_data.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("not.existing.util.rule.and.enum", "1")
        assertEventDataAccepted(validator, elg, "data_1", "AAA")
        assertEventDataAccepted(validator, elg, "data_1", "BBB")
        assertEventDataAccepted(validator, elg, "data_1", "CCC")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.REJECTED, "data_1", "DDD")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.REJECTED, "data_1", "FIRST")
    }

    @Test
    fun test_validate_event_data_with_third_party_rule_and_enum() {
        val validator = newValidatorByFile("test_rules_list_event_data.json", utilProducer = TestUtilRuleProducer())
        val elg = LogEventGroup("third.party.util.rule.and.enum", "1")
        assertEventDataAccepted(validator, elg, "data_1", "AAA")
        assertEventDataAccepted(validator, elg, "data_1", "BBB")
        assertEventDataAccepted(validator, elg, "data_1", "CCC")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.REJECTED, "data_1", "DDD")
        assertEventDataAccepted(validator, elg, "data_1", "FIRST")
        assertEventDataNotAccepted(validator, elg, ValidationResultType.REJECTED, "data_1", "SECOND")
    }

    @Test
    fun test_validate_object_list_event_data() {
        val validator = newValidatorByFile("test_object_event_data.json")
        val eventLogGroup = LogEventGroup("object.group", "1")

        val data =
            hashMapOf<String, Any>("obj" to listOf(hashMapOf("name" to "AAA"), hashMapOf("name" to "NOT_DEFINED")))

        val validatedEventData =
            validator.validateEvent(createLogEvent(eventLogGroup, data = data))
        assertNotNull(validatedEventData)
        val objData = validatedEventData.event.data["obj"] as List<*>
        assertEquals(2, objData.size)
        val elements = objData.map { it as Map<*, *> }
        assertEquals("AAA", elements[0]["name"])
        assertEquals(ValidationResultType.REJECTED.description, elements[1]["name"])

    }

    @Test
    fun test_validate_nested_objects_event_data() {
        val validator = newValidatorByFile("test_nested_object_event_data.json")
        val eventLogGroup = LogEventGroup("object.group", "1")

        val data = hashMapOf<String, Any>(
            "obj" to hashMapOf(
                "nested_obj" to hashMapOf(
                    "name" to "NOT_DEFINED",
                    "count" to "1"
                )
            )
        )

        val validatedEventData =
            validator.validateEvent(createLogEvent(eventLogGroup, data = data))
        assertNotNull(validatedEventData)
        val objData = validatedEventData.event.data["obj"] as Map<*, *>
        val nestedObj = objData["nested_obj"] as Map<*, *>
        assertEquals(ValidationResultType.REJECTED.description, nestedObj["name"])
        assertEquals("1", nestedObj["count"])
    }

    @Test
    fun test_list_validation() {
        val validator = newValidatorByFile("test_list_validation.json")
        val eventLogGroup = LogEventGroup("object.group", "1")

        val data = hashMapOf<String, Any>("elements" to listOf("NOT_DEFINED", "AAA"))

        val validatedEventData =
            validator.validateEvent(createLogEvent(eventLogGroup, data = data))
        assertNotNull(validatedEventData)
        val elements = validatedEventData.event.data["elements"] as List<*>
        assertEquals(2, elements.size)
        assertEquals(ValidationResultType.REJECTED.description, elements[0])
        assertEquals("AAA", elements[1])
    }

    @Test
    fun test_object_validation_with_custom_rule() {
        val validator = newValidatorByFile("test_object_with_custom_rule.json", utilProducer = TestUtilRuleProducer())
        val eventLogGroup = LogEventGroup("object.group", "1")
        val data = hashMapOf<String, Any>(
            "obj" to hashMapOf(
                "id_1" to TestCustomActionId.FIRST.name,
                "id_2" to "NOT_DEFINED"
            )
        )
        val validatedEventData =
            validator.validateEvent(createLogEvent(eventLogGroup, data = data))
        assertNotNull(validatedEventData)
        val objData = validatedEventData.event.data["obj"] as Map<*, *>
        assertEquals(TestCustomActionId.FIRST.name, objData["id_1"])
        assertEquals(ValidationResultType.REJECTED.description, objData["id_2"])
    }

    @Test
    fun test_validate_if_metadata_unreachable() {
        val validator = createUnreachableValidator()
        val eventLogGroup = LogEventGroup("object.group", "1")
        val data: HashMap<String, Any> = hashMapOf("count" to 1)
        val validatedEventData =
            validator.validateEvent(createLogEvent(eventLogGroup, data = data))
        assertNotNull(validatedEventData)

        assertEquals(
            ValidationResultType.UNREACHABLE_METADATA.description,
            validatedEventData.event.data[ValidationResultType.UNREACHABLE_METADATA.description]
        )
    }

    @Test
    fun test_validate_rule_undefined() {
        val validator = newValidatorByFile("test_empty_rule.json")
        val eventLogGroup = LogEventGroup("build.gradle.actions", "1")
        val data: HashMap<String, Any> = hashMapOf("count" to 1)
        val validatedEventData =
            validator.validateEvent(createLogEvent(eventLogGroup, data = data))
        assertNotNull(validatedEventData)
        assertEquals(
            ValidationResultType.UNDEFINED_RULE.description,
            validatedEventData.event.data[ValidationResultType.UNDEFINED_RULE.description]
        )
    }

    private fun createUnreachableValidator(): SensitiveDataValidator<ValidationRuleStorage<EventLogBuild>> {
        val storage = object : ValidationRuleStorage<EventLogBuild> {
            override fun isUnreachable(): Boolean = true

            override fun getGroupValidators(groupId: String): GroupValidators<EventLogBuild> =
                GroupValidators(null, null)
        }
        return SensitiveDataValidator(storage)
    }

    private fun assertEventAccepted(
        validator: SensitiveDataValidator<SimpleValidationRuleStorage<EventLogBuild>>,
        eventLogGroup: LogEventGroup,
        eventId: String
    ) {
        val actual = validator.validateEvent(createLogEvent(eventLogGroup, eventId))
        assertNotNull(actual)
        assertEquals(eventId, actual.event.id, message = "Validation failed for $eventId")
    }

    private fun assertIncorrectRule(
        validator: SensitiveDataValidator<SimpleValidationRuleStorage<EventLogBuild>>,
        eventLogGroup: LogEventGroup,
        eventId: String
    ) {
        val logEvent = validator.validateEvent(createLogEvent(eventLogGroup, eventId))
        assertNotNull(logEvent)
        assertEquals(ValidationResultType.INCORRECT_RULE.description, logEvent.event.id)
    }

    private fun assertThirdPartyRule(
        validator: SensitiveDataValidator<SimpleValidationRuleStorage<EventLogBuild>>,
        eventLogGroup: LogEventGroup,
        eventId: String
    ) {
        val logEvent = validator.validateEvent(createLogEvent(eventLogGroup, eventId))
        assertNotNull(logEvent)
        assertEquals(ValidationResultType.THIRD_PARTY.description, logEvent.event.id)
    }

    private fun assertEventRejected(
        validator: SensitiveDataValidator<SimpleValidationRuleStorage<EventLogBuild>>,
        eventLogGroup: LogEventGroup,
        eventId: String
    ) {
        val actual = validator.validateEvent(createLogEvent(eventLogGroup, eventId))
        assertNotNull(actual)
        assertEquals(ValidationResultType.REJECTED.description, actual.event.id)
    }

    private fun assertEventDataAccepted(
        validator: SensitiveDataValidator<SimpleValidationRuleStorage<EventLogBuild>>,
        eventLogGroup: LogEventGroup,
        key: String,
        dataValue: String
    ) {
        val data: HashMap<String, Any> = hashMapOf(key to dataValue)
        val logEvent = createLogEvent(eventLogGroup, data = data)
        val validatedEvent = validator.validateEvent(logEvent)
        assertNotNull(validatedEvent)
        assertEquals(data, validatedEvent.event.data)
    }

    private fun assertEventDataNotAccepted(
        validator: SensitiveDataValidator<SimpleValidationRuleStorage<EventLogBuild>>,
        eventLogGroup: LogEventGroup,
        resultType: ValidationResultType,
        key: String,
        dataValue: String
    ) {
        val data: HashMap<String, Any> = hashMapOf(key to dataValue)
        val logEvent = createLogEvent(eventLogGroup, data = data)
        val validatedEvent = validator.validateEvent(logEvent)
        assertNotNull(validatedEvent)
        val validationResult = validatedEvent.event.data.entries.first().value
        assertEquals(resultType.description, validationResult)
    }

    @Suppress("unused")
    internal enum class TestCustomActionId { FIRST, SECOND, THIRD }

    open class LocalEnumCustomValidationRule(private val values: Collection<String>) :
        UtilValidationRule {

        override fun validate(data: String, context: EventContext): ValidationResultType {
            if (values.contains(data)) return ValidationResultType.ACCEPTED
            return ValidationResultType.REJECTED
        }
    }

    private fun createEventLog(eventData: HashMap<String, *>) = newLogEvent(
        session = "80bb576ed123",
        build = "203.6682.168",
        bucket = "123",
        time = System.currentTimeMillis(),
        groupId = "groupId",
        groupVersion = "42",
        recorderVersion = "1",
        eventId = "eventId",
        isState = true,
        eventData = eventData
    )

    private fun createGroupDescriptors(eventData: Map<String, Set<String>>? = null): EventGroupRemoteDescriptors {
        val groupDescriptors = EventGroupRemoteDescriptors()
        val groupDescriptor = EventGroupRemoteDescriptors.EventGroupRemoteDescriptor()
        groupDescriptor.id = "groupId"
        groupDescriptor.versions!!.add(EventGroupRemoteDescriptors.GroupVersionRange("0", null))
        val groupRemoteRule = EventGroupRemoteDescriptors.GroupRemoteRule()
        groupRemoteRule.event_id = setOf("eventId")
        groupRemoteRule.event_data = eventData
        groupDescriptor.rules = groupRemoteRule
        groupDescriptors.groups.add(groupDescriptor)
        return groupDescriptors
    }

    private fun createLogEvent(
        eventLogGroup: LogEventGroup,
        eventId: String = "test.event",
        data: HashMap<String, Any> = HashMap()
    ): LogEvent {
        return LogEvent(
            "testSession",
            "191.4812",
            "123",
            System.currentTimeMillis(),
            eventLogGroup,
            "1",
            LogEventAction(eventId, data = data)
        )
    }

    private class TestLocalEnumCustomValidationRule :
        LocalEnumCustomValidationRule(listOf("FIRST", "SECOND", "THIRD"))

    private class TestExistingValidationRule :
        LocalEnumCustomValidationRule(listOf("FIRST", "SECOND", "THIRD"))

    private class TestThirdPartyValidationRule : UtilValidationRule {
        override fun validate(data: String, context: EventContext): ValidationResultType {
            return if (data == "FIRST") ValidationResultType.ACCEPTED else ValidationResultType.THIRD_PARTY
        }
    }

    class TestUtilRuleProducer : UtilRuleProducer() {
        override fun createValidationRule(value: String, contextData: EventGroupContextData): UtilValidationRule? {
            return when (value) {
                "existing_rule" -> TestExistingValidationRule()
                "third_party_rule" -> TestThirdPartyValidationRule()
                else -> null
            }
        }
    }


}