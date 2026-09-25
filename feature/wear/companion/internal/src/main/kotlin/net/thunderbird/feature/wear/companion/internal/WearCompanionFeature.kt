package net.thunderbird.feature.wear.companion.internal

/**
 * Whether the Wear OS companion is turned on, by the `wear_companion` feature flag.
 *
 * While it's off, nothing is published to the watch and its requests are answered as unsupported.
 */
internal fun interface WearCompanionFeature {
    fun isEnabled(): Boolean
}
