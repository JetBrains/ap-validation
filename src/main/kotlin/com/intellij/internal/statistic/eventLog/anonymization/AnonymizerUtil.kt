package com.intellij.internal.statistic.eventLog.anonymization

import com.intellij.internal.statistic.eventLog.util.StringUtil
import java.security.MessageDigest

object AnonymizerUtil {
  private val sha256 by lazy { MessageDigest.getInstance("SHA-256") }

  fun anonymize(salt: String, data: String): String = anonymize(salt.toByteArray(), data)

  fun anonymize(salt: ByteArray, data: String): String {
    if (data.isBlank()) {
      return data
    }
    val md = cloneDigest(sha256)
    md.update(salt)
    md.update(data.toByteArray())
    return StringUtil.toHexString(md.digest())
  }

  /**
   * Digest cloning is faster than requesting a new one from [MessageDigest.getInstance].
   * This approach is used in Guava as well.
   */
  private fun cloneDigest(digest: MessageDigest): MessageDigest {
    return try {
      digest.clone() as MessageDigest
    } catch (e: CloneNotSupportedException) {
      throw IllegalArgumentException("Message digest is not cloneable: $this")
    }
  }
}