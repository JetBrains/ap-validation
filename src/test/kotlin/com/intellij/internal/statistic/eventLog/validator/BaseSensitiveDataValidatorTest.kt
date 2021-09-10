// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator

import com.google.gson.Gson
import com.intellij.internal.statistic.eventLog.EventLogBuild
import com.intellij.internal.statistic.eventLog.validator.rules.utils.UtilRuleProducer
import com.intellij.internal.statistic.eventLog.validator.rules.utils.ValidationSimpleRuleFactory
import com.jetbrains.fus.reporting.model.metadata.EventGroupRemoteDescriptors
import java.io.File
import kotlin.test.assertTrue

abstract class BaseSensitiveDataValidatorTest {

    internal fun newValidatorByFile(fileName: String,
                                    excludedFields: List<String> = emptyList(),
                                    utilProducer: UtilRuleProducer = ValidationSimpleRuleFactory.REJECTING_UTIL_URL_PRODUCER
    ): SensitiveDataValidator<SimpleValidationRuleStorage<EventLogBuild>> {
        return newValidator(loadMetadata(fileName), excludedFields, utilProducer)
    }

    internal fun newValidator(
        content: String,
        excludedFields: List<String> = emptyList(),
        utilProducer: UtilRuleProducer = ValidationSimpleRuleFactory.REJECTING_UTIL_URL_PRODUCER
    ): SensitiveDataValidator<SimpleValidationRuleStorage<EventLogBuild>> {
        val groupDescriptors = Gson().fromJson(content, EventGroupRemoteDescriptors::class.java)
        return SensitiveDataValidator(
            SimpleValidationRuleStorage(
                groupDescriptors,
                EventLogBuild.EVENT_LOG_BUILD_PRODUCER,
                excludedFields = excludedFields,
                utilRulesProducer = utilProducer
            )
        )
    }

    private fun loadMetadata(fileName: String): String {
        val url = BaseSensitiveDataValidatorTest::class.java.classLoader.getResource("validation/$fileName")
        assertTrue(url != null, "Unable to find file: $fileName")
        val file = File(url.toURI())
        assertTrue(file.exists())
        return file.readText()
    }
}
