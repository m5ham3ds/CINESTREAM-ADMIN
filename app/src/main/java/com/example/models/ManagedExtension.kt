package com.example.models

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Modern Managed Extension Lifecycle States (Phase C2).
 * Supported states: ACTIVE, MAINTENANCE, DISABLED, DEPRECATED.
 */
enum class ManagedExtensionStatus {
    ACTIVE,
    MAINTENANCE,
    DISABLED,
    DEPRECATED;

    companion object {
        fun fromString(value: String?): ManagedExtensionStatus {
            return when (value?.trim()?.uppercase()) {
                "ACTIVE" -> ACTIVE
                "MAINTENANCE" -> MAINTENANCE
                "DISABLED" -> DISABLED
                "DEPRECATED" -> DEPRECATED
                else -> DISABLED // Safe fallback: never silently convert unknown to ACTIVE
            }
        }
    }
}

/**
 * Standard Capabilities supported by CineStream bundled scrapers.
 */
object ManagedExtensionCapabilities {
    const val SEARCH = "SEARCH"
    const val DETAILS = "DETAILS"
    const val EPISODES = "EPISODES"
    const val SERVER_DISCOVERY = "SERVER_DISCOVERY"
    const val VIDEO_EXTRACTION = "VIDEO_EXTRACTION"
    const val CLOUDFLARE_CHALLENGE = "CLOUDFLARE_CHALLENGE"
    const val ARABIC_DUB = "ARABIC_DUB"
    const val ENGLISH_DUB = "ENGLISH_DUB"

    val ALL = listOf(
        SEARCH,
        DETAILS,
        EPISODES,
        SERVER_DISCOVERY,
        VIDEO_EXTRACTION,
        CLOUDFLARE_CHALLENGE,
        ARABIC_DUB,
        ENGLISH_DUB
    )
}

/**
 * Standard Audio Capabilities supported by CineStream scraper catalog (Phase 09B-A).
 * Allowed values: ARABIC_DUB, ENGLISH_DUB, ORIGINAL_AUDIO, MULTI_AUDIO.
 * Note: language denotes the website/metadata language, while audioCapabilities
 * denotes confirmed playback voiceover tracks.
 */
object ManagedExtensionAudioCapabilities {
    const val ARABIC_DUB = "ARABIC_DUB"
    const val ENGLISH_DUB = "ENGLISH_DUB"
    const val ORIGINAL_AUDIO = "ORIGINAL_AUDIO"
    const val MULTI_AUDIO = "MULTI_AUDIO"

    val ALL = listOf(
        ARABIC_DUB,
        ENGLISH_DUB,
        ORIGINAL_AUDIO,
        MULTI_AUDIO
    )
}

/**
 * Standard Content Types supported by CineStream scraper catalog.
 */
object ManagedExtensionContentTypes {
    const val MOVIE = "MOVIE"
    const val SERIES = "SERIES"
    const val ANIME = "ANIME"
    const val ASIAN_DRAMA = "ASIAN_DRAMA"
    const val ARABIC_DUB = "ARABIC_DUB"
    const val ENGLISH_DUB = "ENGLISH_DUB"

    val ALL = listOf(
        MOVIE,
        SERIES,
        ANIME,
        ASIAN_DRAMA,
        ARABIC_DUB,
        ENGLISH_DUB
    )
}

/**
 * Canonical Managed Extension Configuration Model (Phase C2 & Phase 09B-A).
 *
 * Stored at: /managed_extensions/{extensionId}
 *
 * Invariant: Pure configuration datastore only.
 * Contains ZERO executable bytecode, APK binaries, DexClassLoader references,
 * or arbitrary JavaScript scripts. Scraper key is strictly an opaque identifier
 * mapping to bundled trusted Kotlin scrapers in the Users App.
 */
@IgnoreExtraProperties
data class ManagedExtension(
    val extensionId: String = "",
    val scraperKey: String = "",
    val name: String = "",
    val description: String = "",
    val baseUrl: String = "",
    val searchUrl: String = "",
    val iconUrl: String = "",
    val runtimeApiVersion: Int = 1,
    val definitionVersion: Int = 1,
    val minAppVersionCode: Int = 1,
    val status: String = ManagedExtensionStatus.ACTIVE.name,
    val priority: Int = 100,
    val language: String = "ar",
    val capabilities: List<String> = emptyList(),
    val contentTypes: List<String> = emptyList(),
    val audioCapabilities: List<String> = emptyList(),
    val enabled: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val statusEnum: ManagedExtensionStatus
        get() = ManagedExtensionStatus.fromString(status)

    val isOperational: Boolean
        get() = enabled && statusEnum == ManagedExtensionStatus.ACTIVE

    val hasConfirmedArabicDub: Boolean
        get() = audioCapabilities.any { it.trim().equals(ManagedExtensionAudioCapabilities.ARABIC_DUB, ignoreCase = true) }

    val hasConfirmedEnglishDub: Boolean
        get() = audioCapabilities.any { it.trim().equals(ManagedExtensionAudioCapabilities.ENGLISH_DUB, ignoreCase = true) }

    val isAudioCapabilityConfirmed: Boolean
        get() = audioCapabilities.isNotEmpty()
}
