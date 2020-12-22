// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.eventLog.validator.rules;

public final class PayloadKey<T> {
  private final String myKey;

  public PayloadKey(String key) {myKey = key;}

  public String getKey() {
    return myKey;
  }
}
