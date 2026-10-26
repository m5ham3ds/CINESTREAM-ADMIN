package com.example.repository

import com.example.models.ManagedExtension
import com.example.models.ManagedExtensionCapabilities
import com.example.models.ManagedExtensionStatus

/**
 * Legacy in-code reference for the expected original source IDs and regression tests.
 * Runtime sync/restore payloads now come ONLY from the versioned-by-content Firestore
 * collection /extension_source_baseline; this object is never used as a restore fallback.
 *
 * Important: the raw Users App bundle has no configured searchUrl or explicit
 * capabilities override. Those keys are intentionally omitted from the raw
 * Firestore payload so the Users App uses each bundled scraper's native search
 * URL and its trusted implementation capabilities.
 */
object OriginalUserAppExtensionDefaults {
    const val BASELINE_UPDATED_AT: Long = 1710000000000L

    private val runtimeCapabilities = listOf(
        ManagedExtensionCapabilities.SEARCH,
        ManagedExtensionCapabilities.DETAILS,
        ManagedExtensionCapabilities.EPISODES,
        ManagedExtensionCapabilities.SERVER_DISCOVERY,
        ManagedExtensionCapabilities.VIDEO_EXTRACTION
    )

    fun getDefaults(): List<ManagedExtension> = listOf(
        ManagedExtension(
            extensionId = "qfilm",
            scraperKey = "qfilm",
            name = "كيو فيلم",
            description = "المشاهدة والتحميل عبر كيو فيلم المدار رسمياً",
            baseUrl = "https://a.qfilm.tv",
            iconUrl = "",
            runtimeApiVersion = 1,
            definitionVersion = 1,
            minAppVersionCode = 1,
            status = ManagedExtensionStatus.ACTIVE.name,
            priority = 110,
            language = "ar",
            // Exact current Users App bundle categories, not the old Admin defaults.
            contentTypes = listOf("MOVIE", "ANIME"),
            capabilities = runtimeCapabilities,
            audioCapabilities = listOf("ARABIC_DUB", "ORIGINAL_AUDIO"),
            enabled = true,
            createdAt = 0L,
            updatedAt = BASELINE_UPDATED_AT
        ),
        ManagedExtension(
            extensionId = "egydead",
            scraperKey = "egydead",
            name = "EgyDead (Managed)",
            description = "المشاهدة والتحميل عبر إيجي ديد المدار رسمياً",
            baseUrl = "https://tv10.egydead.live",
            iconUrl = "",
            runtimeApiVersion = 1,
            definitionVersion = 1,
            minAppVersionCode = 1,
            status = ManagedExtensionStatus.ACTIVE.name,
            priority = 100,
            language = "ar",
            contentTypes = listOf("MOVIE", "SERIES", "ANIME"),
            capabilities = runtimeCapabilities,
            audioCapabilities = listOf("ARABIC_DUB", "ORIGINAL_AUDIO"),
            enabled = true,
            createdAt = 0L,
            updatedAt = BASELINE_UPDATED_AT
        ),
        ManagedExtension(
            extensionId = "witanime",
            scraperKey = "witanime",
            name = "WitAnime",
            description = "المشاهدة عبر ويت أنمي المدار رسمياً",
            baseUrl = "https://witanime.site",
            iconUrl = "",
            runtimeApiVersion = 1,
            definitionVersion = 1,
            minAppVersionCode = 1,
            status = ManagedExtensionStatus.ACTIVE.name,
            priority = 105,
            language = "ar",
            contentTypes = listOf("ANIME"),
            capabilities = runtimeCapabilities,
            audioCapabilities = emptyList(),
            enabled = true,
            createdAt = 0L,
            updatedAt = BASELINE_UPDATED_AT
        ),
        ManagedExtension(
            extensionId = "anime4up",
            scraperKey = "anime4up",
            name = "Anime4Up",
            description = "المشاهدة عبر أنمي فور أب المدار رسمياً",
            baseUrl = "https://w1.anime4up.rest",
            iconUrl = "",
            runtimeApiVersion = 1,
            definitionVersion = 1,
            minAppVersionCode = 1,
            status = ManagedExtensionStatus.ACTIVE.name,
            priority = 108,
            language = "ar",
            contentTypes = listOf("ANIME"),
            capabilities = runtimeCapabilities,
            audioCapabilities = emptyList(),
            enabled = true,
            createdAt = 0L,
            updatedAt = BASELINE_UPDATED_AT
        ),
        ManagedExtension(
            extensionId = "animeblkom",
            scraperKey = "animeblkom",
            name = "AnimeBlkom",
            description = "المشاهدة عبر أنمي بالكوم المدار رسمياً",
            baseUrl = "https://animeblkom.net",
            iconUrl = "",
            runtimeApiVersion = 1,
            definitionVersion = 1,
            minAppVersionCode = 1,
            status = ManagedExtensionStatus.ACTIVE.name,
            priority = 106,
            language = "ar",
            contentTypes = listOf("ANIME"),
            capabilities = runtimeCapabilities,
            audioCapabilities = emptyList(),
            enabled = true,
            createdAt = 0L,
            updatedAt = BASELINE_UPDATED_AT
        )
    )

    val canonicalIds: Set<String>
        get() = getDefaults().map { it.extensionId.lowercase() }.toSet()

    /**
     * Serialize only the fields present in the Users App's original bundled
     * configuration. This deliberately performs a full-document replacement
     * without `searchUrl`, explicit `capabilities`, or `createdAt`.
     */
    fun toRawFirestorePayload(extension: ManagedExtension): Map<String, Any?> = linkedMapOf(
        "id" to extension.extensionId,
        "extensionId" to extension.extensionId,
        "scraperKey" to extension.scraperKey,
        "name" to extension.name,
        "description" to extension.description,
        "baseUrl" to extension.baseUrl,
        "iconUrl" to extension.iconUrl,
        "runtimeApiVersion" to extension.runtimeApiVersion,
        "definitionVersion" to extension.definitionVersion,
        "minAppVersionCode" to extension.minAppVersionCode,
        "status" to extension.status,
        "priority" to extension.priority,
        "language" to extension.language,
        "contentTypes" to extension.contentTypes,
        "audioCapabilities" to extension.audioCapabilities,
        "enabled" to extension.enabled,
        "updatedAt" to BASELINE_UPDATED_AT
    )
}
