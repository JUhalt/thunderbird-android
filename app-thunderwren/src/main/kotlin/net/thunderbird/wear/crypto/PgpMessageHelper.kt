package net.thunderbird.wear.crypto

object PgpMessageHelper {

    private const val PGP_HEADER = "-----BEGIN PGP MESSAGE-----"
    private const val PGP_FOOTER = "-----END PGP MESSAGE-----"

    fun isPgpEncrypted(bodyText: String): Boolean {
        return bodyText.contains(PGP_HEADER) && bodyText.contains(PGP_FOOTER)
    }

    fun formatPgpSummary(bodyText: String): String {
        return if (isPgpEncrypted(bodyText)) {
            "🔒 [OpenPGP Encrypted Message]\nDecryption key synced from Thunderbird Companion."
        } else {
            bodyText
        }
    }
}
