// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator.rules.beans;

import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.FUSRule;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.beans.EventDataField;
import com.intellij.internal.statistic.eventLog.validator.rules.utils.ValidationSimpleRuleFactory;
import com.intellij.internal.statistic.eventLog.validator.storage.GlobalRulesHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.intellij.internal.statistic.eventLog.validator.ValidationResultType.*;
import static com.intellij.internal.statistic.eventLog.validator.rules.utils.ValidationSimpleRuleFactory.REJECTING_UTIL_URL_PRODUCER;
import static com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors.EventGroupRemoteDescriptor;
import static com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors.GroupRemoteRule;

public final class EventGroupRules {
  public static final EventGroupRules EMPTY =
    new EventGroupRules(Collections.emptySet(), Collections.emptyMap(), EventGroupContextData.EMPTY,
                        new ValidationSimpleRuleFactory(REJECTING_UTIL_URL_PRODUCER), Collections.emptyList());

  private final FUSRule[] eventIdRules;
  private final Map<String, FUSRule[]> eventDataRules = new ConcurrentHashMap<>();
  private final List<String> VALIDATION_TYPES =
    Arrays.stream(values()).map(ValidationResultType::getDescription).collect(Collectors.toList());
  private final List<String> myExcludedFields;

  private EventGroupRules(@Nullable Set<String> eventIdRules,
                          @Nullable Map<String, Set<String>> eventDataRules,
                          @NotNull EventGroupContextData contextData,
                          @NotNull ValidationSimpleRuleFactory factory,
                          @NotNull List<String> excludedFields) {
    myExcludedFields = excludedFields;
    this.eventIdRules = factory.getRules(eventIdRules, contextData);

    if (eventDataRules != null) {
      for (Map.Entry<String, Set<String>> entry : eventDataRules.entrySet()) {
        if (myExcludedFields.contains(entry.getKey())) {
          this.eventDataRules.put(entry.getKey(), new FUSRule[]{FUSRule.TRUE});
        }
        else {
          this.eventDataRules.put(entry.getKey(), factory.getRules(entry.getValue(), contextData));
        }
      }
    }
  }

  public FUSRule[] getEventIdRules() {
    return eventIdRules;
  }

  public Map<String, FUSRule[]> getEventDataRules() {
    return eventDataRules;
  }

  public boolean areEventIdRulesDefined() {
    return eventIdRules.length > 0;
  }

  public boolean areEventDataRulesDefined() {
    return eventDataRules.size() > 0;
  }

  public ValidationResultType validateEventId(@NotNull EventContext context) {
    ValidationResultType prevResult = null;
    if (VALIDATION_TYPES.contains(context.eventId)) return ACCEPTED;
    for (FUSRule rule : eventIdRules) {
      ValidationResultType resultType = rule.validate(context.eventId, context);
      if (resultType.isFinal()) return resultType;
      prevResult = resultType;
    }
    return prevResult != null ? prevResult : REJECTED;
  }

  /**
   * @return validated data, incorrect values are replaced with {@link ValidationResultType#getDescription()}
   */
  public EventDataField validateEventData(@NotNull String key,
                                          @Nullable Object data,
                                          @NotNull EventContext context) {
    return validateEventData(key, data, context, key);
  }

  private EventDataField validateEventData(@NotNull String key,
                                           @Nullable Object data,
                                           @NotNull EventContext context,
                                           @NotNull String fieldName) {
    if (data == null) return new EventDataField(fieldName, REJECTED.getDescription());
    if (data instanceof String && VALIDATION_TYPES.contains(data)) return new EventDataField(fieldName, data);
    if (myExcludedFields.contains(key)) return new EventDataField(fieldName, data);

    if (data instanceof Map<?, ?>) {
      HashMap<Object, Object> validatedData = new HashMap<>();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) data).entrySet()) {
        Object entryKey = entry.getKey();
        if (entryKey instanceof String) {
          EventDataField field = validateEventData(key + "." + entryKey, entry.getValue(), context, (String) entryKey);
          validatedData.put(field.getName(), field.getValue());
        } else {
          validatedData.put(entryKey, REJECTED.getDescription());
        }
      }
      String validatedFieldName = fieldName;
      if (!validatedData.isEmpty() &&
        validatedData.keySet().stream().allMatch(value -> value instanceof String && UNDEFINED_RULE.getDescription().equals(value))) {
        validatedFieldName = UNDEFINED_RULE.getDescription();
      }
      return new EventDataField(validatedFieldName, validatedData);
    }

    if (data instanceof List<?>) {
      List<Object> validatedData = new ArrayList<>();
      List<String> fieldNames = new ArrayList<>();
      for (Object value : ((List<?>) data)) {
        EventDataField validatedField = validateEventData(key, value, context, fieldName);
        validatedData.add(validatedField.getValue());
        fieldNames.add(validatedField.getName());
      }
      String validatedFieldName = fieldName;
      if (!validatedData.isEmpty() &&
        fieldNames.stream().allMatch(value -> UNDEFINED_RULE.getDescription().equals(value))) {
        validatedFieldName = UNDEFINED_RULE.getDescription();
      }
      return new EventDataField(validatedFieldName, validatedData);
    }

    FUSRule[] rules = eventDataRules.get(key);
    if (rules == null || rules.length == 0) {
      return new EventDataField(UNDEFINED_RULE.getDescription(), UNDEFINED_RULE.getDescription());
    }
    return new EventDataField(fieldName, validateValue(data, context, rules));
  }

  private static Object validateValue(@NotNull Object data, @NotNull EventContext context, FUSRule @NotNull [] rules) {
    ValidationResultType resultType = acceptRule(data.toString(), context, rules);
    return resultType == ACCEPTED ? data : resultType.getDescription();
  }

  private static ValidationResultType acceptRule(@NotNull String ruleData, @NotNull EventContext context, FUSRule @Nullable ... rules) {
    if (rules == null) return UNDEFINED_RULE;

    ValidationResultType prevResult = null;
    for (FUSRule rule : rules) {
      ValidationResultType resultType = rule.validate(ruleData, context);
      if (resultType.isFinal()) return resultType;
      prevResult = resultType;
    }
    return prevResult != null ? prevResult : REJECTED;
  }

  public static @NotNull EventGroupRules create(@NotNull EventGroupRemoteDescriptor group,
                                                @NotNull GlobalRulesHolder globalRulesHolder,
                                                @NotNull ValidationSimpleRuleFactory factory,
                                                @NotNull List<String> excludedFields) {
    GroupRemoteRule rules = group.rules;
    return rules == null
           ? EMPTY
           : new EventGroupRules(rules.event_id, rules.event_data,
                                 new EventGroupContextData(rules.enums, rules.regexps, globalRulesHolder), factory, excludedFields);
  }
}
