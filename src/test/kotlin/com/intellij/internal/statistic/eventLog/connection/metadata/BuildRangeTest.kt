package com.intellij.internal.statistic.eventLog.connection.metadata

import com.intellij.internal.statistic.eventLog.connection.metadata.EventGroupFilterRules.BuildRange
import com.jetbrains.fus.reporting.model.lion3.BuildNumber
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildRangeTest {
    @Test
    fun contains() {
        assertTrue(BuildRange(BuildNumber("1.2"), BuildNumber("2.0")).contains(BuildNumber("1.2")))
        assertFalse(BuildRange(BuildNumber("1.2"), BuildNumber("2.0")).contains(BuildNumber("2.0")))
        assertTrue(BuildRange(BuildNumber("1.2"), BuildNumber("2.0")).contains(BuildNumber("1.3")))
        assertFalse(BuildRange(BuildNumber("1.2"), BuildNumber("2.0")).contains(BuildNumber("1.1")))
        assertFalse(BuildRange(BuildNumber("1.2"), BuildNumber("2.0")).contains(BuildNumber("2.0.1")))

        assertTrue(BuildRange(BuildNumber("1.2"), null).contains(BuildNumber("1.2")))
        assertTrue(BuildRange(BuildNumber("1.2"), null).contains(BuildNumber("1.2.1")))
        assertFalse(BuildRange(BuildNumber("1.2"), null).contains(BuildNumber("1.1")))
    }
}