package com.intellij.internal.statistic.eventLog;

import com.intellij.internal.statistic.eventLog.EventLogBuild;
import com.intellij.internal.statistic.eventLog.validator.SensitiveDataValidator;
import com.intellij.internal.statistic.eventLog.validator.ValidationRuleStorage;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.beans.EventGroupRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class TestValidatorReturningImmutableData extends SensitiveDataValidator<ValidationRuleStorage<EventLogBuild>> {
    public TestValidatorReturningImmutableData(@NotNull ValidationRuleStorage<EventLogBuild> validationRulesStorage) {
        super(validationRulesStorage);
    }

    @NotNull
    @Override
    protected Map<String, Object> guaranteeCorrectEventData(@NotNull EventContext context, @Nullable EventGroupRules groupRules) {
        return Collections.emptyMap();
    }
}
