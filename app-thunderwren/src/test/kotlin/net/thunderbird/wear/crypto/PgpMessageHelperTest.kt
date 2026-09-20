package net.thunderbird.wear.crypto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class PgpMessageHelperTest {

    private val testSubject = PgpMessageHelper

    @Test
    fun `isPgpEncrypted returns true when message contains PGP headers`() {
        // Arrange
        val encryptedBody = """
            -----BEGIN PGP MESSAGE-----
            Version: OpenPGP.js
            hQEMA12345
            -----END PGP MESSAGE-----
        """.trimIndent()

        // Act
        val result = testSubject.isPgpEncrypted(encryptedBody)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `isPgpEncrypted returns false when message is plain text`() {
        // Arrange
        val plainTextBody = "Hello world, this is a normal email."

        // Act
        val result = testSubject.isPgpEncrypted(plainTextBody)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `formatPgpSummary formats encrypted messages with lock badge`() {
        // Arrange
        val encryptedBody = """
            -----BEGIN PGP MESSAGE-----
            Version: OpenPGP.js
            hQEMA12345
            -----END PGP MESSAGE-----
        """.trimIndent()

        // Act
        val result = testSubject.formatPgpSummary(encryptedBody)

        // Assert
        assertThat(result).isEqualTo("🔒 [OpenPGP Encrypted Message]\nDecryption key synced from Thunderbird Companion.")
    }
}
