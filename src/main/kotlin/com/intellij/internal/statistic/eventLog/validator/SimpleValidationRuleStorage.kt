// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator

import com.intellij.internal.statistic.eventLog.connection.metadata.EventGroupsFilterRules
import com.intellij.internal.statistic.eventLog.connection.metadata.EventLogBuildParser
import com.intellij.internal.statistic.eventLog.validator.rules.beans.EventGroupRules
import com.intellij.internal.statistic.eventLog.validator.rules.utils.UtilRuleProducer
import com.intellij.internal.statistic.eventLog.validator.rules.utils.ValidationSimpleRuleFactory
import com.intellij.internal.statistic.eventLog.validator.storage.GlobalRulesHolder
import com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors

/**
 * @param initialMetadata validation rules that will be used initially, they can be updated later (see [update]).
 * @param excludedFields  list of event data fields to be excluded from validation.
 */

class SimpleValidationRuleStorage<T : Comparable<T>?> @JvmOverloads constructor(initialMetadata: EventGroupRemoteDescriptors,
                                                                                private val buildParser: EventLogBuildParser<T>,
                                                                                private val excludedFields: List<String> = emptyList(),
                                                                                utilRulesProducer: UtilRuleProducer = ValidationSimpleRuleFactory.REJECTING_UTIL_URL_PRODUCER) : ValidationRuleStorage<T> {
  private val validationRuleFactory = ValidationSimpleRuleFactory(utilRulesProducer)
  private val eventsValidators: MutableMap<String?, EventGroupRules> = HashMap() // guarded by lock
  private lateinit var filterRules: EventGroupsFilterRules<T> // guarded by lock
  private val lock = Any()

  init {
    updateEventGroupRules(initialMetadata)
  }

  fun update(metadata: EventGroupRemoteDescriptors) {
    updateEventGroupRules(metadata)
  }

  override fun getGroupValidators(groupId: String): GroupValidators<T> {
    synchronized(lock) {
      return GroupValidators(eventsValidators[groupId], filterRules)
    }
  }

  private fun updateEventGroupRules(descriptors: EventGroupRemoteDescriptors) {
    synchronized(lock) {
      eventsValidators.clear()
      eventsValidators.putAll(createValidators(descriptors))
      filterRules = EventGroupsFilterRules.create(descriptors, buildParser)
    }
  }

  private fun createValidators(descriptors: EventGroupRemoteDescriptors): Map<String?, EventGroupRules> {
    val globalRulesHolder = GlobalRulesHolder(descriptors.rules)
    val groups = descriptors.groups
    return groups.associate { it.id to EventGroupRules.create(it, globalRulesHolder, validationRuleFactory, excludedFields) }
  }

  override fun isUnreachable(): Boolean = false
}

data class GroupValidators<T : Comparable<T>?>(
  val eventGroupRules: EventGroupRules?,
  val versionFilter: EventGroupsFilterRules<T>?
)