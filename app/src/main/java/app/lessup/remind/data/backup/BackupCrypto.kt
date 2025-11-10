package app.lessup.remind.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets

internal object BackupCrypto {
    private val MAGIC = byteArrayOf('L'.code.toByte(), 'R'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())
    private const val FLAG_ENCRYPTED: Int = 1
    private const val GCM_TAG_BITS = 128
    private const val IV_LENGTH = 12

    fun wrapPlain(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.write(MAGIC)
            out.writeInt(1) // version
            out.writeInt(0) // flags
            out.writeInt(data.size)
            out.write(data)
        }
        return bos.toByteArray()
    }

    fun encrypt(data: ByteArray, password: String): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(data)
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.write(MAGIC)
            out.writeInt(1)
            out.writeInt(FLAG_ENCRYPTED)
            out.writeInt(iv.size)
            out.write(iv)
            out.writeInt(encrypted.size)
            out.write(encrypted)
        }
        return bos.toByteArray()
    }

    fun decrypt(bytes: ByteArray, password: String?): ByteArray {
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val magic = ByteArray(MAGIC.size)
            input.readFully(magic)
            require(magic.contentEquals(MAGIC)) { "Invalid backup signature" }
            val version = input.readInt()
            require(version == 1) { "Unsupported backup version" }
            val flags = input.readInt()
            return if (flags and FLAG_ENCRYPTED == FLAG_ENCRYPTED) {
                require(!password.isNullOrEmpty()) { "Password required" }
                val ivLength = input.readInt()
                val iv = ByteArray(ivLength)
                input.readFully(iv)
                val payloadLength = input.readInt()
                val payload = ByteArray(payloadLength)
                input.readFully(payload)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, deriveKey(password), GCMParameterSpec(GCM_TAG_BITS, iv))
                cipher.doFinal(payload)
            } else {
                val length = input.readInt()
                val payload = ByteArray(length)
                input.readFully(payload)
                payload
            }
        }
    }

    private fun deriveKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        val keyBytes = bytes.copyOf(16)
        return SecretKeySpec(keyBytes, "AES")
    }
}
