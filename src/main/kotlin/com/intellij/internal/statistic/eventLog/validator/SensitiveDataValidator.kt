// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator

import com.intellij.internal.statistic.eventLog.EventLogSystemEvents
import com.intellij.internal.statistic.eventLog.newLogEvent
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.beans.EventGroupRules
import com.intellij.internal.statistic.eventLog.validator.rules.impl.beans.EventDataField
import com.jetbrains.fus.reporting.model.lion3.LogEvent

/**
 * Validates log event according to remote groups validation rules.
 * Used to ensure that no personal or proprietary data is recorded.
 */
open class SensitiveDataValidator<S: ValidationRuleStorage<*>>(val validationRulesStorage: S) {
  /**
   * @return null if the build or version failed validation,
   * otherwise returns validated event in which incorrect values are replaced with {@link ValidationResultType#getDescription()}.
   */
  fun validateEvent(event: LogEvent): LogEvent? {
    val logEventAction = event.event
    return validate(event.group.id, event.group.version, event.build, event.session, event.bucket, event.time,
                    event.recorderVersion,
                    logEventAction.id,
                    logEventAction.data, logEventAction.state,
                    logEventAction.count)
  }

  fun validate(groupId: String,
               groupVersion: String,
               build: String,
               sessionId: String,
               bucket: String,
               eventTime: Long,
               recorderVersion: String,
               eventId: String,
               data: Map<String, Any>,
               isState: Boolean,
               count: Int = 1): LogEvent? {
    val (groupRules, versionFilter) = validationRulesStorage.getGroupValidators(groupId)
    if (versionFilter != null && !versionFilter.accepts(groupId, groupVersion, build)) {
      return null
    }
    val context = EventContext.create(eventId, data)
    val validatedEventId = guaranteeCorrectEventId(context, groupRules)
    val validatedEventData = guaranteeCorrectEventData(context, groupRules)
    return newLogEvent(sessionId, build, bucket, eventTime, groupId, groupVersion, recorderVersion, validatedEventId, isState, validatedEventData, count)
  }

  protected open fun guaranteeCorrectEventId(context: EventContext, groupRules: EventGroupRules?): String {
    if (validationRulesStorage.isUnreachable()) return ValidationResultType.UNREACHABLE_METADATA.description
    if (EventLogSystemEvents.SYSTEM_EVENTS.contains(context.eventId)) return context.eventId
    val validationResultType = validateEvent(context, groupRules)
    return if (validationResultType == ValidationResultType.ACCEPTED) context.eventId else validationResultType.description
  }

  protected open fun guaranteeCorrectEventData(context: EventContext,
                                               groupRules: EventGroupRules?): MutableMap<String, Any> {
    val validatedData: MutableMap<String, Any> = HashMap()
    for ((key, entryValue) in context.eventData) {
      val (validatedFieldName, validateEventData) = validateEventData(context, groupRules, key, entryValue)
      validatedData[validatedFieldName] = validateEventData
    }
    return validatedData
  }

  private fun validateEventData(context: EventContext,
                                groupRules: EventGroupRules?,
                                key: String,
                                entryValue: Any): EventDataField {
    if (validationRulesStorage.isUnreachable()) return EventDataField(ValidationResultType.UNREACHABLE_METADATA.description,
      ValidationResultType.UNREACHABLE_METADATA.description)
    return if (groupRules == null) return EventDataField(ValidationResultType.UNDEFINED_RULE.description,
      ValidationResultType.UNDEFINED_RULE.description)
    else groupRules.validateEventData(key, entryValue, context)
  }

  companion object {
    @JvmStatic
    fun validateEvent(context: EventContext, groupRules: EventGroupRules?): ValidationResultType {
      return if (groupRules == null || !groupRules.areEventIdRulesDefined()) {
        ValidationResultType.UNDEFINED_RULE // there are no rules (eventId and eventData) to validate
      }
      else groupRules.validateEventId(context)
    }
  }

}