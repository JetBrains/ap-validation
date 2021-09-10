// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator

import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SensitiveDataValidatorCreationTest : BaseSensitiveDataValidatorTest() {
    private fun doTest(content: String, version: String, build: String, vararg expected: String) {
        val validator = newValidator(content)
        for (exp in expected) {
            val groupValidators = validator.validationRulesStorage.getGroupValidators(exp)
            val groupRules = groupValidators.eventGroupRules
            val versionFilter = groupValidators.versionFilter
            assertNotNull(versionFilter)
            assertTrue(versionFilter.accepts(exp, version, build))

            assertNotNull(groupRules)
            assertTrue(groupRules.areEventIdRulesDefined())
        }
    }

    private fun doTestNotContains(content: String, version: String, build: String, vararg expected: String) {
        val validator = newValidator(content)
        for (exp in expected) {
            val groupValidators = validator.validationRulesStorage.getGroupValidators(exp)
            val versionFilter = groupValidators.versionFilter
            assertNotNull(versionFilter)
            assertTrue(!versionFilter.accepts(exp, version, build))

            val groupRules = groupValidators.eventGroupRules
            assertNotNull(groupRules)
            assertTrue(groupRules.areEventIdRulesDefined())
        }
    }

    @Test
    fun `test creating simple validator`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.1234"
    }],
    "versions" : [ {
      "from" : "2",
      "to" : "5"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","183.1234.31", "test.group.id")
    }

    @Test
    fun `test creating validator with build`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content,"3", "183.1234.31", "test.group.id")
    }

    @Test
    fun `test creating validator with version`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "versions" : [ {
      "from" : "2",
      "to" : "5"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","183.1234.31", "test.group.id")
    }

    @Test
    fun `test creating validator with version out of range`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "versions" : [ {
      "from" : "12",
      "to" : "15"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "13","183.1234.31", "test.group.id")
    }

    @Test
    fun `test creating validator without build and version`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","183.1234.31", "test.group.id")
    }

    @Test
    fun `test creating validator with build equals to from`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","183.1234", "test.group.id")
    }

    @Test
    fun `test creating validator with build older than from`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","183.2234", "test.group.id")
    }

    @Test
    fun `test creating validator with major build older than from`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","191.12", "test.group.id")
    }

    @Test
    fun `test creating validator with build before from`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","183.12", "test.group.id")
    }

    @Test
    fun `test creating validator with major build before from`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "191.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","183.12", "test.group.id")
    }

    @Test
    fun `test creating validator with build equals to to`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "to" : "192.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","192.1234", "test.group.id")
    }

    @Test
    fun `test creating validator with build before to`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "to" : "192.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","192.12", "test.group.id")
    }

    @Test
    fun `test creating validator with major build before to`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "to" : "192.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","191.12", "test.group.id")
    }

    @Test
    fun `test creating validator with build after to`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "to" : "192.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","192.2421", "test.group.id")
    }

    @Test
    fun `test creating validator with major build after to`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "to" : "192.1234"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","202.124", "test.group.id")
    }

    @Test
    fun `test creating validator with build between from and to`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.351",
      "to" : "191.235.124"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","191.12", "test.group.id")
    }

    @Test
    fun `test creating validator with major build between from and to`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.351",
      "to" : "202.235.124"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","201.42", "test.group.id")
    }

    @Test
    fun `test creating validator with multiple ranges`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.351",
      "to" : "191.235.124"
    }, {
      "from" : "192.21",
      "to" : "202.235.124"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTest(content, "3","201.42", "test.group.id")
    }

    @Test
    fun `test creating validator with build before range`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.351",
      "to" : "202.235.124"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","183.42", "test.group.id")
    }

    @Test
    fun `test creating validator with major build before range`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.351",
      "to" : "202.235.124"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","181.42", "test.group.id")
    }

    @Test
    fun `test creating validator with build after range`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.351",
      "to" : "202.235.124"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","202.421", "test.group.id")
    }

    @Test
    fun `test creating validator with build major after range`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
      "from" : "183.351",
      "to" : "202.235.124"
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","203.41", "test.group.id")
    }

    @Test
    fun `test creating validator when from and to are not set`() {
        val content = """
{
  "groups" : [{
    "id" : "test.group.id",
    "title" : "Test Group",
    "description" : "Test group description",
    "type" : "counter",
    "builds" : [ {
    }],
    "rules" : {
      "event_id" : [ "{enum:foo|bar|baz}" ]
    }
  }]
}
    """.trimIndent()

        doTestNotContains(content, "3","203.41", "test.group.id")
    }
}