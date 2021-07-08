// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator

import com.intellij.internal.statistic.eventLog.EventLogSystemEvents
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.beans.EventGroupRules
import com.intellij.internal.statistic.eventLog.validator.rules.impl.beans.EventDataField
import com.jetbrains.fus.reporting.model.lion3.*
import com.jetbrains.fus.reporting.model.lion4.FusReport

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
    val (validatedEventId, validatedEventData) = validate(event.group.id,
            event.group.version,
            event.build,
            logEventAction.id,
            logEventAction.data) ?: return null
    val validatedEvent = LogEventAction(validatedEventId, logEventAction.state, validatedEventData, logEventAction.count)
    return LogEvent(event.session,
            event.build,
            event.bucket,
            event.time,
            LogEventGroup(event.group.id, event.group.version),
            event.recorderVersion,
            validatedEvent)
  }

  fun validateEvent(event: com.jetbrains.fus.reporting.model.lion4.LogEvent): com.jetbrains.fus.reporting.model.lion4.LogEvent? {
    val (validatedEventId, validatedEventData) = validate(event.group.id,
            event.group.version.toString(),
            event.build,
            event.event.id,
            event.event.data) ?: return null
    val validatedEvent = com.jetbrains.fus.reporting.model.lion4.LogEventAction(validatedEventId, validatedEventData, event.event.count)
    return com.jetbrains.fus.reporting.model.lion4.LogEvent(event.recorder,
            event.product,
            event.ids,
            event.internal,
            event.time,
            event.build,
            event.session,
            event.group,
            event.bucket,
            validatedEvent,
            event.system_data)
  }

  /**
   * @return null if request doesn't contain valid events,
   * otherwise returns validated request in which incorrect values in events are replaced with {@link ValidationResultType#getDescription()}.
   */
  fun validateReport(report: ValidatedFusReport): ValidatedFusReport? {
    val records = report.records.mapNotNull { rec ->
      val safeEvents = rec.events.mapNotNull { validateEvent(it) }
      if (safeEvents.isNotEmpty()) ValidatedFusRecord(safeEvents) else null
    }
    if (records.isEmpty()) return null
    return ValidatedFusReport(report.product, report.device, report.recorder, report.internal, records)
  }

  /**
   * @return null if request doesn't contain valid events,
   * otherwise returns validated request in which incorrect values in events are replaced with {@link ValidationResultType#getDescription()}.
   */
  fun validateReport(report: FusReport): FusReport? {
    val safeEvents = report.events.mapNotNull { validateEvent(it) }
    return if (safeEvents.isNotEmpty()) FusReport(safeEvents) else null
  }

  private fun validate(groupId: String,
                       groupVersion: String,
                       build: String,
                       eventId: String,
                       data: Map<String, Any>): ValidatedEvent? {
    val (groupRules, versionFilter) = validationRulesStorage.getGroupValidators(groupId)
    if (versionFilter != null && !versionFilter.accepts(groupId, groupVersion, build)) {
      return null
    }
    val context = EventContext.create(eventId, data)
    val validatedEventId = guaranteeCorrectEventId(context, groupRules)
    val validatedEventData = guaranteeCorrectEventData(context, groupRules)
    return ValidatedEvent(validatedEventId, validatedEventData)
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
      } else groupRules.validateEventId(context)
    }
  }

  private data class ValidatedEvent(
          val eventId: String,
          val eventData: MutableMap<String, Any>
  )
}