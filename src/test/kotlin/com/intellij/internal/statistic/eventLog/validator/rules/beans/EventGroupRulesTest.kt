// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator.rules.beans

import com.intellij.internal.statistic.eventLog.validator.ValidationResultType
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.utils.ValidationSimpleRuleFactory
import com.intellij.internal.statistic.eventLog.validator.rules.utils.ValidationSimpleRuleFactory.REJECTING_UTIL_URL_PRODUCER
import com.intellij.internal.statistic.eventLog.validator.storage.GlobalRulesHolder
import com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors
import org.junit.Test
import kotlin.test.assertEquals

class EventGroupRulesTest {
  @Test
  fun `test skip validation for system fields`() {
    val descriptors: EventGroupRemoteDescriptors = getMetadataContent()
    val globalRulesHolder = GlobalRulesHolder(descriptors.rules)
    val fieldToExclude = "system_event_id"
    val systemFieldValue = "123"
    val groupRules = EventGroupRules.create(descriptors.groups.first(),
                                            globalRulesHolder,
                                            ValidationSimpleRuleFactory(REJECTING_UTIL_URL_PRODUCER),
                                            listOf(fieldToExclude))
    val eventContext = EventContext.create("test.event.id", mapOf(fieldToExclude to systemFieldValue))
    val (validatedFields, validatedEventData) = groupRules.validateEventData(fieldToExclude, systemFieldValue, eventContext)
    assertEquals(systemFieldValue, validatedEventData)
    assertEquals(fieldToExclude, validatedFields)
  }

  @Test
  fun `test skip validation for already validated field`() {
    val descriptors: EventGroupRemoteDescriptors = getMetadataContent()
    val globalRulesHolder = GlobalRulesHolder(descriptors.rules)
    val groupRules = EventGroupRules.create(descriptors.groups.first(),
                                            globalRulesHolder,
                                            ValidationSimpleRuleFactory(REJECTING_UTIL_URL_PRODUCER),
                                            emptyList())
    val fieldName = "count"
    val fieldValue = ValidationResultType.THIRD_PARTY.description
    val eventContext = EventContext.create("test.event.id", mapOf(fieldName to fieldValue))
    val (validatedFields, validatedEventData) = groupRules.validateEventData(fieldName, fieldValue, eventContext)
    assertEquals(fieldValue, validatedEventData)
    assertEquals(fieldName, validatedFields)
  }

  @Test
  fun `test skip validation for already validated field in list`() {
    val descriptors: EventGroupRemoteDescriptors = getMetadataContent()
    val globalRulesHolder = GlobalRulesHolder(descriptors.rules)
    val groupRules = EventGroupRules.create(descriptors.groups.first(),
                                            globalRulesHolder,
                                            ValidationSimpleRuleFactory(REJECTING_UTIL_URL_PRODUCER),
                                            emptyList())
    val fieldName = "count"
    val fieldValue = listOf(ValidationResultType.THIRD_PARTY.description, ValidationResultType.UNREACHABLE_METADATA.description)
    val eventContext = EventContext.create("test.event.id", mapOf(fieldName to fieldValue))
    val (validatedFields, validatedEventData) = groupRules.validateEventData(fieldName, fieldValue, eventContext)
    assertEquals(fieldValue, validatedEventData)
    assertEquals(fieldName, validatedFields)
  }

  private fun getMetadataContent(): EventGroupRemoteDescriptors {
    val descriptors = EventGroupRemoteDescriptors()
    val element = EventGroupRemoteDescriptors.EventGroupRemoteDescriptor()
    element.id = "actions"
    element.versions?.add(EventGroupRemoteDescriptors.GroupVersionRange("1", null))
    val rules = EventGroupRemoteDescriptors.GroupRemoteRule()
    rules.event_id = hashSetOf("{util#action}")
    element.rules = rules
    descriptors.groups.add(element)
    return descriptors
  }

}