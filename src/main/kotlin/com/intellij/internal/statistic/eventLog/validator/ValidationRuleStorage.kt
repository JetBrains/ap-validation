// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator

import com.intellij.internal.statistic.eventLog.validator.rules.beans.EventGroupRules
import com.intellij.internal.statistic.eventLog.validator.rules.utils.ValidationSimpleRuleFactory
import com.intellij.internal.statistic.eventLog.validator.storage.GlobalRulesHolder
import com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors

interface ValidationRuleStorage<T : Comparable<T>?> {
    fun getGroupValidators(groupId: String): GroupValidators<T>

    fun isUnreachable(): Boolean

    fun createValidators(
        descriptors: EventGroupRemoteDescriptors,
        validationSimpleRuleFactory: ValidationSimpleRuleFactory,
        excludeFields: List<String>
    ): Map<String?, EventGroupRules> {
        val globalRulesHolder = GlobalRulesHolder(descriptors.rules)
        val groups = descriptors.groups
        return groups.associate {
            it.id to EventGroupRules.create(
                it, globalRulesHolder, validationSimpleRuleFactory, excludeFields
            )
        }
    }

}