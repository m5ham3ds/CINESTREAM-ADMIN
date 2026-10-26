package com.example.models

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Lifecycle states for dubbing provider groups (Phase 09B-A).
 * Allowed values: ACTIVE, MAINTENANCE, DISABLED.
 */
enum class DubbingGroupStatus {
    ACTIVE,
    MAINTENANCE,
    DISABLED;

    companion object {
        fun fromString(value: String?, defaultForEnglish: Boolean = false): DubbingGroupStatus {
            return when (value?.trim()?.uppercase()) {
                "ACTIVE" -> ACTIVE
                "MAINTENANCE" -> MAINTENANCE
                "DISABLED" -> DISABLED
                else -> if (defaultForEnglish) MAINTENANCE else ACTIVE
            }
        }
    }
}

/**
 * Configuration state for dubbing provider groups (Phase 09B-A).
 * Stored at: /config/search_order.dubbingConfig
 *
 * Invariants:
 * - arabicDubbedStatus defaults to ACTIVE (empty list means no search providers configured).
 * - englishDubbedStatus defaults to MAINTENANCE in all fallback cases.
 */
@IgnoreExtraProperties
data class DubbingConfig(
    val arabicDubbedStatus: String = DubbingGroupStatus.ACTIVE.name,
    val englishDubbedStatus: String = DubbingGroupStatus.MAINTENANCE.name
) {
    val arabicStatusEnum: DubbingGroupStatus
        get() = DubbingGroupStatus.fromString(arabicDubbedStatus, defaultForEnglish = false)

    val englishStatusEnum: DubbingGroupStatus
        get() = DubbingGroupStatus.fromString(englishDubbedStatus, defaultForEnglish = true)

    fun toMap(): Map<String, Any?> = mapOf(
        "arabicDubbedStatus" to arabicDubbedStatus,
        "englishDubbedStatus" to englishDubbedStatus
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): DubbingConfig {
            if (map == null) return DubbingConfig()
            val rawArabic = map["arabicDubbedStatus"] as? String
                ?: map["arabicDubStatus"] as? String
                ?: map["arabic_dubbed_status"] as? String
            val rawEnglish = map["englishDubbedStatus"] as? String
                ?: map["englishDubStatus"] as? String
                ?: map["english_dubbed_status"] as? String

            val arStatus = DubbingGroupStatus.fromString(rawArabic, defaultForEnglish = false).name
            // In all fallback cases, missing or unverified english status resolves to MAINTENANCE
            val enStatus = DubbingGroupStatus.fromString(rawEnglish, defaultForEnglish = true).name

            return DubbingConfig(
                arabicDubbedStatus = arStatus,
                englishDubbedStatus = enStatus
            )
        }
    }
}

/**
 * Content-Type Categories supported for independent Search Ordering (Phase EXT-CANONICAL-01 & Phase 09B-A).
 *
 * Each category maps to standard contentTypes defined in [ManagedExtensionContentTypes].
 */
enum class SearchOrderCategory(val canonicalType: String, val labelAr: String, val labelEn: String) {
    MOVIE(ManagedExtensionContentTypes.MOVIE, "الأفلام", "Movies"),
    TV(ManagedExtensionContentTypes.SERIES, "المسلسلات", "TV Series"),
    ANIME(ManagedExtensionContentTypes.ANIME, "الأنمي", "Anime"),
    ARABIC_DUB(ManagedExtensionContentTypes.ARABIC_DUB, "الدبلجة العربية", "Arabic Dubbed"),
    ENGLISH_DUB(ManagedExtensionContentTypes.ENGLISH_DUB, "الدبلجة الإنجليزية (صيانة)", "English Dubbed (Maintenance)");

    companion object {
        fun fromString(value: String?): SearchOrderCategory {
            return when (value?.trim()?.uppercase()) {
                "MOVIE", "MOVIES" -> MOVIE
                "TV", "SERIES", "TV_SERIES", "SHOWS" -> TV
                "ANIME" -> ANIME
                "ARABIC_DUB", "ARABIC_DUBBED", "ARABIC_DUBBING", "ARABIC" -> ARABIC_DUB
                "ENGLISH_DUB", "ENGLISH_DUBBED", "ENGLISH_DUBBING", "ENGLISH" -> ENGLISH_DUB
                else -> MOVIE
            }
        }
    }
}

/**
 * Canonical Search Order Configuration (Phase EXT-CANONICAL-01 & Phase 09B-A).
 *
 * Stored at: /config/search_order
 *
 * Invariant: Pure configuration containing ordered lists of stable extension IDs.
 * Independent ordering for MOVIE, TV (Series), ANIME, ARABIC_DUB, and ENGLISH_DUB.
 * List lengths are unlimited (positions 1, 2, 3, 4, ...).
 * No executable bytecode, no dynamic class loading.
 */
@IgnoreExtraProperties
data class SearchOrderConfig(
    val movie: List<String> = emptyList(),
    val tv: List<String> = emptyList(),
    val anime: List<String> = emptyList(),
    val arabicDubbed: List<String> = emptyList(),
    val englishDubbed: List<String> = emptyList(),
    val dubbingConfig: DubbingConfig = DubbingConfig(),
    val updatedAt: Long = 0L,
    val updatedBy: String = ""
) {
    // Backward compatibility aliases
    val arabicDub: List<String> get() = arabicDubbed
    val englishDub: List<String> get() = englishDubbed

    /**
     * Retrieve the ordered list of extension IDs for a given category.
     */
    fun getOrderForCategory(category: SearchOrderCategory): List<String> {
        return when (category) {
            SearchOrderCategory.MOVIE -> movie
            SearchOrderCategory.TV -> tv
            SearchOrderCategory.ANIME -> anime
            SearchOrderCategory.ARABIC_DUB -> arabicDubbed
            SearchOrderCategory.ENGLISH_DUB -> englishDubbed
        }
    }

    /**
     * Return a new [SearchOrderConfig] with updated ordering for the specified category.
     */
    fun withOrderForCategory(category: SearchOrderCategory, newOrder: List<String>): SearchOrderConfig {
        val sanitized = newOrder.map { it.trim() }.filter { it.isNotBlank() }
        return when (category) {
            SearchOrderCategory.MOVIE -> copy(movie = sanitized, updatedAt = System.currentTimeMillis())
            SearchOrderCategory.TV -> copy(tv = sanitized, updatedAt = System.currentTimeMillis())
            SearchOrderCategory.ANIME -> copy(anime = sanitized, updatedAt = System.currentTimeMillis())
            SearchOrderCategory.ARABIC_DUB -> copy(arabicDubbed = sanitized, updatedAt = System.currentTimeMillis())
            SearchOrderCategory.ENGLISH_DUB -> copy(englishDubbed = sanitized, updatedAt = System.currentTimeMillis())
        }
    }

    /**
     * Return a new [SearchOrderConfig] with updated dubbing configuration status.
     */
    fun withDubbingConfig(newConfig: DubbingConfig): SearchOrderConfig {
        return copy(dubbingConfig = newConfig, updatedAt = System.currentTimeMillis())
    }

    /**
     * Serialize to Firestore document map.
     * Writes exact canonical fields:
     * - movie
     * - tv (and "series" compatibility alias)
     * - anime
     * - arabicDubbed (and "arabicDub" / "arabic_dub" dual-write aliases)
     * - englishDubbed (and "englishDub" / "english_dub" dual-write aliases)
     * - dubbingConfig: { arabicDubbedStatus, englishDubbedStatus }
     */
    fun toMap(adminEmail: String = ""): Map<String, Any?> {
        val now = if (updatedAt > 0L) updatedAt else System.currentTimeMillis()
        val email = adminEmail.ifBlank { updatedBy }
        return mapOf(
            "movie" to movie,
            "tv" to tv,
            "series" to tv, // Alias for scraper engine compatibility
            "anime" to anime,
            "arabicDubbed" to arabicDubbed,
            "arabicDub" to arabicDubbed, // Backward-compat alias
            "arabic_dub" to arabicDubbed, // Backward-compat alias
            "englishDubbed" to englishDubbed,
            "englishDub" to englishDubbed, // Backward-compat alias
            "english_dub" to englishDubbed, // Backward-compat alias
            "dubbingConfig" to dubbingConfig.toMap(),
            "updatedAt" to now,
            "updatedBy" to email
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>?): SearchOrderConfig {
            if (map == null) return SearchOrderConfig()

            val rawMovie = map["movie"]
            val rawTv = map["tv"] ?: map["series"]
            val rawAnime = map["anime"]
            val rawArabicDubbed = map["arabicDubbed"]
                ?: map["arabicDub"]
                ?: map["arabic_dub"]
                ?: map["arabic_dubbing"]
                ?: map["arabicDubbing"]
            val rawEnglishDubbed = map["englishDubbed"]
                ?: map["englishDub"]
                ?: map["english_dub"]
                ?: map["english_dubbing"]
                ?: map["englishDubbing"]

            val movieIds = parseIdList(rawMovie)
            val tvIds = parseIdList(rawTv)
            val animeIds = parseIdList(rawAnime)
            val arabicDubbedIds = parseIdList(rawArabicDubbed)
            val englishDubbedIds = parseIdList(rawEnglishDubbed)

            val rawDubbingConfig = map["dubbingConfig"] as? Map<String, Any?>
            val parsedDubbingConfig = DubbingConfig.fromMap(rawDubbingConfig)

            val updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L
            val updatedBy = (map["updatedBy"] as? String) ?: ""

            return SearchOrderConfig(
                movie = movieIds,
                tv = tvIds,
                anime = animeIds,
                arabicDubbed = arabicDubbedIds,
                englishDubbed = englishDubbedIds,
                dubbingConfig = parsedDubbingConfig,
                updatedAt = updatedAt,
                updatedBy = updatedBy
            )
        }

        fun fromDocument(doc: DocumentSnapshot?): SearchOrderConfig {
            if (doc == null || !doc.exists()) return SearchOrderConfig()
            return fromMap(doc.data)
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseIdList(raw: Any?): List<String> {
            return when (raw) {
                is List<*> -> raw.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
                is String -> raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }
        }
    }
}
