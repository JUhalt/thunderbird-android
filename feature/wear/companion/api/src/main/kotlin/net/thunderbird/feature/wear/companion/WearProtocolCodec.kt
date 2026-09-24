package net.thunderbird.feature.wear.companion

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Encodes and decodes the Data Layer payloads exchanged between the phone and the watch. */
object WearProtocolCodec {
    private val json = Json {
        // Allows an older app to read payloads that gained new fields.
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    fun encodeSnapshot(snapshot: WearInboxSnapshot): ByteArray = encode(WearInboxSnapshot.serializer(), snapshot)

    /** Returns `null` if [bytes] isn't a snapshot this version understands. */
    fun decodeSnapshot(bytes: ByteArray): WearInboxSnapshot? {
        val snapshot = decode(WearInboxSnapshot.serializer(), bytes) ?: return null
        return snapshot.takeIf { it.version == WearCompanion.PROTOCOL_VERSION }
    }

    fun encodeRequest(request: WearRequest): ByteArray = encode(WearRequest.serializer(), request)

    /** Returns `null` if [bytes] isn't a request this version understands. */
    fun decodeRequest(bytes: ByteArray): WearRequest? = decode(WearRequest.serializer(), bytes)

    fun encodeResponse(response: WearResponse): ByteArray = encode(WearResponse.serializer(), response)

    /** Returns `null` if [bytes] isn't a response this version understands. */
    fun decodeResponse(bytes: ByteArray): WearResponse? = decode(WearResponse.serializer(), bytes)

    private fun <T> encode(serializer: KSerializer<T>, value: T): ByteArray {
        return json.encodeToString(serializer, value).encodeToByteArray()
    }

    private fun <T> decode(serializer: KSerializer<T>, bytes: ByteArray): T? {
        return try {
            json.decodeFromString(serializer, bytes.decodeToString())
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
