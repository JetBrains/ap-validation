package com.intellij.internal.statistic.eventLog

import com.intellij.internal.statistic.eventLog.validator.ValidationResultType
import org.junit.Test
import kotlin.test.assertEquals

class StatisticsEventEscaperTest {
  @Test
  fun `test not escape validation result types`() {
    val undefinedRule = ValidationResultType.UNDEFINED_RULE.description
    assertEquals(undefinedRule, StatisticsEventEscaper.escapeFieldName(undefinedRule))
  }

  @Test
  fun `test escape event field name`() {
    assertEquals("field_name", StatisticsEventEscaper.escapeFieldName("field.name"))
  }
}