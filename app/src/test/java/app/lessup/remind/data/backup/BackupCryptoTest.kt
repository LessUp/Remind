package app.lessup.remind.data.backup

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class BackupCryptoTest {
    @Test
    fun `wrap plain preserves payload`() {
        val payload = "hello".toByteArray()
        val wrapped = BackupCrypto.wrapPlain(payload)
        val decoded = BackupCrypto.decrypt(wrapped, null)
        assertContentEquals(payload, decoded)
    }

    @Test
    fun `encrypt and decrypt with password`() {
        val payload = ByteArray(64) { it.toByte() }
        val encrypted = BackupCrypto.encrypt(payload, "secret")
        val decrypted = BackupCrypto.decrypt(encrypted, "secret")
        assertContentEquals(payload, decrypted)
    }

    @Test
    fun `decrypt fails with missing password`() {
        val payload = BackupCrypto.encrypt("data".toByteArray(), "pw")
        assertFailsWith<IllegalArgumentException> { BackupCrypto.decrypt(payload, null) }
    }
}
