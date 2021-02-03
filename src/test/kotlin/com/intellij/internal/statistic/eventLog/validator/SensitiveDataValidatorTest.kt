package com.intellij.internal.statistic.eventLog.validator

import com.intellij.internal.statistic.eventLog.EventLogBuild.EVENT_LOG_BUILD_PRODUCER
import com.intellij.internal.statistic.eventLog.connection.metadata.EventGroupRemoteDescriptors
import com.intellij.internal.statistic.eventLog.newLogEvent
import org.junit.Test
import kotlin.test.assertEquals

class SensitiveDataValidatorTest {
    @Test
    fun `test validation fails with empty remote descriptors`() {
        val sensitiveDataValidator =
            SensitiveDataValidator(SimpleValidationRuleStorage(EventGroupRemoteDescriptors(), EVENT_LOG_BUILD_PRODUCER))
        val validatedEvent = sensitiveDataValidator.validateEvent(newLogEvent(session = "80bb576ed123",
            build = "203.6682.168",
            bucket = "123",
            time = System.currentTimeMillis(),
            groupId = "groupId",
            groupVersion = "42",
            recorderVersion = "1",
            eventId = "eventId",
            isState = true,
            eventData = hashMapOf("count" to 42)))
        assertEquals(null, validatedEvent)
    }
}