package com.intellij.internal.statistic.eventLog.validator

import com.intellij.internal.statistic.eventLog.EventLogBuild.EVENT_LOG_BUILD_PRODUCER
import com.intellij.internal.statistic.eventLog.connection.metadata.EventGroupRemoteDescriptors
import com.intellij.internal.statistic.eventLog.newLogEvent
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SensitiveDataValidatorTest {
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

    private fun createEventLog(eventData: HashMap<String, *>) = newLogEvent(session = "80bb576ed123",
        build = "203.6682.168",
        bucket = "123",
        time = System.currentTimeMillis(),
        groupId = "groupId",
        groupVersion = "42",
        recorderVersion = "1",
        eventId = "eventId",
        isState = true,
        eventData = eventData)

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
}