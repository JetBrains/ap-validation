// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator.rules;

import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import org.jetbrains.annotations.NotNull;

/**
 * Used to validate events before recording it locally.
 */
public interface FUSRule {
    FUSRule[] EMPTY_ARRAY = new FUSRule[0];
    FUSRule TRUE = (s,c) -> ValidationResultType.ACCEPTED;
    FUSRule FALSE = (s,c) -> ValidationResultType.REJECTED;

    /**
     * <p>Validates event id and event data before recording it locally. Used to ensure that no personal or proprietary data is recorded.<p/>
     *
     * <ul>
     *     <li>{@link ValidationResultType#ACCEPTED} - data is checked and should be recorded as is;</li>
     *     <li>{@link ValidationResultType#THIRD_PARTY} - data is correct but is implemented in an unknown third-party plugin, e.g. third-party file type<br/>
     *     <li>{@link ValidationResultType#REJECTED} - unexpected data, e.g. cannot find run-configuration by provided id;</li>
     * </ul>
     *
     * @param data what is validated. Event id or the value of event data field.
     * @param context whole event context, i.e. both event id and event data.
     */
    @NotNull
    ValidationResultType validate(@NotNull String data, @NotNull EventContext context);

}
