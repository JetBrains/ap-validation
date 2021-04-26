package com.intellij.internal.statistic.eventLog.anonymization

import org.junit.Test
import kotlin.test.assertEquals

class AnonymizerUtilTest {
  @Test
  fun `test hash sensitive data`() {
    val salt = byteArrayOf(45, 105, 19, -80, 109, 38, 24, -23, 27, -102, -123, 92, 60, -63, -83, -67, -66,
      -17, -26, 44, 123, 28, 40, -74, 77, -105, 105, -41, 36, -55, -21, 5)
    doTestHashing(salt, "test-project-name", "dfa488a68d19d909af416ea02c8013e314562803d421ae747d7fec06dd080609")
    doTestHashing(salt, "", "")
  }

  private fun doTestHashing(salt: ByteArray, data: String, expected: String) {
    val actual = AnonymizerUtil.anonymize(salt, data)
    assertEquals(expected, actual, "Hashing algorithm was changed for '$data'")
  }
}