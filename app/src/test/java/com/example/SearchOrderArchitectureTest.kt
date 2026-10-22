package com.example

import com.example.contract.FirebaseCollections
import com.example.contract.FirebaseConfigDocs
import com.example.models.ManagedExtension
import com.example.models.ManagedExtensionCapabilities
import com.example.models.ManagedExtensionContentTypes
import com.example.models.ManagedExtensionStatus
import com.example.models.SearchOrderCategory
import com.example.models.SearchOrderConfig
import com.example.repository.SearchOrderRepository
import com.example.validation.SearchOrderValidator
import org.junit.Assert.*
import org.junit.Test

/**
 * Focused Unit Tests for Phase EXT-CANONICAL-01:
 * Canonical Extension + Search Order Architecture.
 *
 * Covers:
 * 1. Search Order Model & Deterministic Serialization
 * 2. Content-Type Capability Validation
 * 3. Extension Lifecycle Invariants (Disabled / Re-enabled / Unknown)
 * 4. Repository & Ordering Operations
 * 5. Legacy Priority Decoupling & Dual-Write Invariants
 */
class SearchOrderArchitectureTest {

    // =========================================================================
    // 1. SEARCH ORDER MODEL TESTS
    // =========================================================================

    @Test
    fun testMovieOrderSerialization() {
        val config = SearchOrderConfig(
            movie = listOf("qfilm", "egydead", "akwam", "cinedownload"),
            updatedAt = 1759160000000L,
            updatedBy = "admin@cinestream.com"
        )
        val map = config.toMap()

        assertEquals(listOf("qfilm", "egydead", "akwam", "cinedownload"), map["movie"])
        assertEquals(1759160000000L, map["updatedAt"])
        assertEquals("admin@cinestream.com", map["updatedBy"])

        val restored = SearchOrderConfig.fromMap(map)
        assertEquals(config.movie, restored.movie)
        assertEquals(4, restored.movie.size)
        assertEquals("qfilm", restored.movie[0])
        assertEquals("cinedownload", restored.movie[3])
    }

    @Test
    fun testTvOrderSerializationWithDualWriteAlias() {
        val config = SearchOrderConfig(
            tv = listOf("egydead", "qfilm", "seriestime"),
            updatedAt = 1759160000000L,
            updatedBy = "admin@cinestream.com"
        )
        val map = config.toMap()

        // Verify primary canonical key "tv"
        assertEquals(listOf("egydead", "qfilm", "seriestime"), map["tv"])
        // Verify backward-compatibility dual-write alias "series"
        assertEquals(listOf("egydead", "qfilm", "seriestime"), map["series"])

        // Deserialization with "tv" present
        val restoredFromTv = SearchOrderConfig.fromMap(map)
        assertEquals(config.tv, restoredFromTv.tv)

        // Deserialization fallback when only "series" exists (legacy client payload)
        val legacyDoc = mapOf(
            "series" to listOf("egydead", "qfilm"),
            "movie" to emptyList<String>(),
            "anime" to emptyList<String>()
        )
        val restoredFromSeries = SearchOrderConfig.fromMap(legacyDoc)
        assertEquals(listOf("egydead", "qfilm"), restoredFromSeries.tv)
    }

    @Test
    fun testAnimeOrderSerialization() {
        val config = SearchOrderConfig(
            anime = listOf("witanime", "anime4up", "animeblkom", "okanime"),
            updatedAt = 1759160000000L
        )
        val map = config.toMap()

        assertEquals(listOf("witanime", "anime4up", "animeblkom", "okanime"), map["anime"])

        val restored = SearchOrderConfig.fromMap(map)
        assertEquals(config.anime, restored.anime)
        assertEquals(4, restored.anime.size)
        assertEquals("witanime", restored.anime.first())
        assertEquals("okanime", restored.anime.last())
    }

    @Test
    fun testDeterministicOrderingPreservation() {
        // Invariant: Insertion order is strictly preserved. No arbitrary sorting or shuffling.
        val orderedList = listOf("scraper_delta", "scraper_alpha", "scraper_omega", "scraper_beta", "scraper_gamma")
        val config = SearchOrderConfig(movie = orderedList)

        val map = config.toMap()
        val restored = SearchOrderConfig.fromMap(map)

        assertEquals(orderedList, restored.movie)
        assertEquals("scraper_delta", restored.movie[0])
        assertEquals("scraper_alpha", restored.movie[1])
        assertEquals("scraper_omega", restored.movie[2])
        assertEquals("scraper_beta", restored.movie[3])
        assertEquals("scraper_gamma", restored.movie[4])
    }

    @Test
    fun testEmptyListValidationAndSerialization() {
        // Invariant: Clean initial state with empty lists must be fully valid and deserializable
        val emptyConfig = SearchOrderConfig()
        assertTrue(emptyConfig.movie.isEmpty())
        assertTrue(emptyConfig.tv.isEmpty())
        assertTrue(emptyConfig.anime.isEmpty())

        val map = emptyConfig.toMap()
        val restored = SearchOrderConfig.fromMap(map)
        assertTrue(restored.movie.isEmpty())
        assertTrue(restored.tv.isEmpty())
        assertTrue(restored.anime.isEmpty())

        // Validation against empty catalog should pass without errors
        val errors = SearchOrderValidator.validate(emptyConfig, emptyMap())
        assertTrue("Empty configuration must pass validation", errors.isEmpty())
    }

    @Test
    fun testDuplicateIdsRejectedAcrossAllCategories() {
        val available = mapOf(
            "ext_movie" to ManagedExtension(extensionId = "ext_movie", contentTypes = listOf(ManagedExtensionContentTypes.MOVIE)),
            "ext_tv" to ManagedExtension(extensionId = "ext_tv", contentTypes = listOf(ManagedExtensionContentTypes.SERIES)),
            "ext_anime" to ManagedExtension(extensionId = "ext_anime", contentTypes = listOf(ManagedExtensionContentTypes.ANIME))
        )

        // 1. Duplicate in Movies
        val dupeMovie = SearchOrderConfig(movie = listOf("ext_movie", "ext_movie"))
        val movieErrors = SearchOrderValidator.validate(dupeMovie, available)
        assertTrue(movieErrors.any { it.contains("Duplicate extension ID in Movies") })

        // 2. Duplicate in TV
        val dupeTv = SearchOrderConfig(tv = listOf("ext_tv", "ext_tv"))
        val tvErrors = SearchOrderValidator.validate(dupeTv, available)
        assertTrue(tvErrors.any { it.contains("Duplicate extension ID in TV Series") })

        // 3. Duplicate in Anime
        val dupeAnime = SearchOrderConfig(anime = listOf("ext_anime", "ext_anime"))
        val animeErrors = SearchOrderValidator.validate(dupeAnime, available)
        assertTrue(animeErrors.any { it.contains("Duplicate extension ID in Anime") })
    }

    // =========================================================================
    // 2. CONTENT TYPE VALIDATION TESTS
    // =========================================================================

    @Test
    fun testAnimeOnlyCannotEnterMovieOrder() {
        val catalog = mapOf(
            "witanime" to ManagedExtension(
                extensionId = "witanime",
                name = "WitAnime",
                contentTypes = listOf(ManagedExtensionContentTypes.ANIME)
            )
        )

        val invalidConfig = SearchOrderConfig(movie = listOf("witanime"))
        val errors = SearchOrderValidator.validate(invalidConfig, catalog)

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("does not support content type Movies"))
        assertFalse(SearchOrderValidator.isExtensionEligibleForCategory(catalog["witanime"]!!, SearchOrderCategory.MOVIE))
    }

    @Test
    fun testAnimeOnlyCannotEnterTvOrder() {
        val catalog = mapOf(
            "anime4up" to ManagedExtension(
                extensionId = "anime4up",
                name = "Anime4Up",
                contentTypes = listOf(ManagedExtensionContentTypes.ANIME)
            )
        )

        val invalidConfig = SearchOrderConfig(tv = listOf("anime4up"))
        val errors = SearchOrderValidator.validate(invalidConfig, catalog)

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("does not support content type TV Series"))
        assertFalse(SearchOrderValidator.isExtensionEligibleForCategory(catalog["anime4up"]!!, SearchOrderCategory.TV))
    }

    @Test
    fun testMovieOnlyCannotEnterAnimeOrder() {
        val catalog = mapOf(
            "cinedownload" to ManagedExtension(
                extensionId = "cinedownload",
                name = "CineDownload",
                contentTypes = listOf(ManagedExtensionContentTypes.MOVIE)
            )
        )

        val invalidConfig = SearchOrderConfig(anime = listOf("cinedownload"))
        val errors = SearchOrderValidator.validate(invalidConfig, catalog)

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("does not support content type Anime"))
        assertFalse(SearchOrderValidator.isExtensionEligibleForCategory(catalog["cinedownload"]!!, SearchOrderCategory.ANIME))
    }

    @Test
    fun testTvOnlyCannotEnterMovieOrder() {
        val catalog = mapOf(
            "seriestime" to ManagedExtension(
                extensionId = "seriestime",
                name = "SeriesTime",
                contentTypes = listOf(ManagedExtensionContentTypes.SERIES)
            )
        )

        val invalidConfig = SearchOrderConfig(movie = listOf("seriestime"))
        val errors = SearchOrderValidator.validate(invalidConfig, catalog)

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("does not support content type Movies"))
        assertFalse(SearchOrderValidator.isExtensionEligibleForCategory(catalog["seriestime"]!!, SearchOrderCategory.MOVIE))
    }

    @Test
    fun testMultiCategoryCanEnterAllSupportedLists() {
        val universalScraper = ManagedExtension(
            extensionId = "egydead",
            name = "EgyDead Universal",
            contentTypes = listOf(
                ManagedExtensionContentTypes.MOVIE,
                ManagedExtensionContentTypes.SERIES,
                ManagedExtensionContentTypes.ANIME
            )
        )
        val catalog = mapOf("egydead" to universalScraper)

        assertTrue(SearchOrderValidator.isExtensionEligibleForCategory(universalScraper, SearchOrderCategory.MOVIE))
        assertTrue(SearchOrderValidator.isExtensionEligibleForCategory(universalScraper, SearchOrderCategory.TV))
        assertTrue(SearchOrderValidator.isExtensionEligibleForCategory(universalScraper, SearchOrderCategory.ANIME))

        val validUniversalConfig = SearchOrderConfig(
            movie = listOf("egydead"),
            tv = listOf("egydead"),
            anime = listOf("egydead")
        )

        val errors = SearchOrderValidator.validate(validUniversalConfig, catalog)
        assertTrue("Universal scraper must be valid in all 3 categories simultaneously", errors.isEmpty())
    }

    // =========================================================================
    // 3. EXTENSION LIFECYCLE TESTS
    // =========================================================================

    @Test
    fun testDisabledExtensionDoesNotBecomeExecutableMerelyBecauseInConfiguration() {
        // Architectural invariant: Being in SearchOrderConfig does NOT make a disabled extension executable.
        // Execution safety is governed by the extension's status and operational flags.
        val disabledExt = ManagedExtension(
            extensionId = "qfilm",
            name = "QFilm",
            status = ManagedExtensionStatus.DISABLED.name,
            enabled = false,
            contentTypes = listOf(ManagedExtensionContentTypes.MOVIE)
        )
        val maintExt = ManagedExtension(
            extensionId = "egydead",
            name = "EgyDead",
            status = ManagedExtensionStatus.MAINTENANCE.name,
            enabled = true,
            contentTypes = listOf(ManagedExtensionContentTypes.MOVIE)
        )

        assertFalse("Disabled extension must not be operational", disabledExt.isOperational)
        assertFalse("Maintenance extension must not be operational", maintExt.isOperational)

        // Configuration preserves them in order
        val config = SearchOrderConfig(movie = listOf("qfilm", "egydead"))
        val catalog = mapOf("qfilm" to disabledExt, "egydead" to maintExt)

        // Validation permits them to stay configured (so disabling an extension doesn't erase its ordering rank)
        val errors = SearchOrderValidator.validate(config, catalog)
        assertTrue("Disabled extensions are valid in config to preserve administrative priority", errors.isEmpty())

        // Runtime executor filtering simulation:
        val executableScrapers = config.movie
            .mapNotNull { catalog[it] }
            .filter { it.isOperational }

        assertTrue("Runtime executor filters out non-operational extensions despite presence in config", executableScrapers.isEmpty())
    }

    @Test
    fun testReEnabledExtensionPreservesValidOrdering() {
        // Extension starts operational in rank #2
        val ext = ManagedExtension(
            extensionId = "qfilm",
            status = ManagedExtensionStatus.ACTIVE.name,
            enabled = true,
            contentTypes = listOf(ManagedExtensionContentTypes.MOVIE)
        )
        val catalog = mutableMapOf("qfilm" to ext)
        val config = SearchOrderConfig(movie = listOf("source_a", "qfilm", "source_b"))

        // Admin disables it
        val disabledExt = ext.copy(status = ManagedExtensionStatus.DISABLED.name, enabled = false)
        catalog["qfilm"] = disabledExt
        assertFalse(catalog["qfilm"]!!.isOperational)
        // Configuration still has "qfilm" at index 1 (rank #2)
        assertEquals(1, config.movie.indexOf("qfilm"))

        // Admin later re-enables it
        val reEnabledExt = disabledExt.copy(status = ManagedExtensionStatus.ACTIVE.name, enabled = true)
        catalog["qfilm"] = reEnabledExt
        assertTrue(catalog["qfilm"]!!.isOperational)

        // Position is preserved at index 1 without needing administrative re-creation
        assertEquals(1, config.movie.indexOf("qfilm"))
        assertEquals(listOf("source_a", "qfilm", "source_b"), config.movie)
    }

    @Test
    fun testDeletedOrUnknownExtensionIdRejected() {
        val catalog = mapOf(
            "known_ext" to ManagedExtension(
                extensionId = "known_ext",
                contentTypes = listOf(ManagedExtensionContentTypes.MOVIE)
            )
        )

        val config = SearchOrderConfig(movie = listOf("known_ext", "deleted_ext_999"))
        val errors = SearchOrderValidator.validate(config, catalog)

        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Extension 'deleted_ext_999' in Movies search order does not exist in registry"))
    }

    // =========================================================================
    // 4. REPOSITORY & ORDERING OPERATIONS
    // =========================================================================

    @Test
    fun testCategoryIsolationOnOrderUpdate() {
        val original = SearchOrderConfig(
            movie = listOf("m1", "m2"),
            tv = listOf("t1", "t2"),
            anime = listOf("a1", "a2")
        )

        // Update TV order only
        val updatedTv = original.withOrderForCategory(SearchOrderCategory.TV, listOf("t2", "t1", "t3"))

        assertEquals(listOf("t2", "t1", "t3"), updatedTv.tv)
        // Movies and Anime MUST remain completely unaffected
        assertEquals(listOf("m1", "m2"), updatedTv.movie)
        assertEquals(listOf("a1", "a2"), updatedTv.anime)
    }

    @Test
    fun testReorderingStepOperations() {
        val list = mutableListOf("pos1", "pos2", "pos3", "pos4")

        // Move Down (pos1 down to pos2)
        val temp = list[0]
        list[0] = list[1]
        list[1] = temp
        assertEquals(listOf("pos2", "pos1", "pos3", "pos4"), list)

        // Move Up (pos3 up to pos1)
        val temp2 = list[2]
        list[2] = list[1]
        list[1] = temp2
        assertEquals(listOf("pos2", "pos3", "pos1", "pos4"), list)

        // Remove item at index 1 ("pos3")
        list.removeAt(1)
        assertEquals(listOf("pos2", "pos1", "pos4"), list)

        // Add new extension
        list.add("pos5")
        assertEquals(listOf("pos2", "pos1", "pos4", "pos5"), list)
    }

    @Test
    fun testUnlimitedListLengthSemantics() {
        // Verify no artificial hard ceiling (e.g. top 3 is not a limit)
        val longList = (1..25).map { "extension_$it" }
        val config = SearchOrderConfig(movie = longList)

        assertEquals(25, config.movie.size)
        val map = config.toMap()
        val restored = SearchOrderConfig.fromMap(map)

        assertEquals(25, restored.movie.size)
        assertEquals("extension_1", restored.movie.first())
        assertEquals("extension_25", restored.movie.last())
    }

    @Test
    fun testDefaultSearchOrderGeneration() {
        val repo = SearchOrderRepository()
        val available = listOf(
            ManagedExtension(extensionId = "anime_top", priority = 200, contentTypes = listOf("ANIME")),
            ManagedExtension(extensionId = "anime_low", priority = 50, contentTypes = listOf("ANIME")),
            ManagedExtension(extensionId = "movie_top", priority = 150, contentTypes = listOf("MOVIE")),
            ManagedExtension(extensionId = "movie_mid", priority = 100, contentTypes = listOf("MOVIE")),
            ManagedExtension(extensionId = "tv_top", priority = 180, contentTypes = listOf("SERIES"))
        )

        val defaults = repo.getDefaultSearchOrder(available)

        assertEquals(listOf("movie_top", "movie_mid"), defaults.movie)
        assertEquals(listOf("tv_top"), defaults.tv)
        assertEquals(listOf("anime_top", "anime_low"), defaults.anime)
    }

    // =========================================================================
    // 5. LEGACY PRIORITY DECOUPLING & CONTRACT INVARIANTS
    // =========================================================================

    @Test
    fun testLegacyPriorityDecoupling() {
        // Extension has legacy global priority = 100
        val extA = ManagedExtension(extensionId = "ext_a", priority = 100, contentTypes = listOf("MOVIE", "SERIES"))
        val extB = ManagedExtension(extensionId = "ext_b", priority = 50, contentTypes = listOf("MOVIE", "SERIES"))

        // In SearchOrderConfig, Admin can place extB FIRST for Movies, and extA FIRST for TV
        val config = SearchOrderConfig(
            movie = listOf("ext_b", "ext_a"),
            tv = listOf("ext_a", "ext_b")
        )

        assertEquals("ext_b", config.movie[0])
        assertEquals("ext_a", config.movie[1])
        assertEquals("ext_a", config.tv[0])
        assertEquals("ext_b", config.tv[1])

        // Verify that ManagedExtension.priority was NOT mutated or overwritten
        assertEquals(100, extA.priority)
        assertEquals(50, extB.priority)
    }

    @Test
    fun testCanonicalPathsIntegrity() {
        assertEquals("config", FirebaseCollections.CONFIG)
        assertEquals("search_order", FirebaseConfigDocs.SEARCH_ORDER)
        assertEquals("managed_extensions", FirebaseCollections.MANAGED_EXTENSIONS)
        assertEquals("extensions", FirebaseCollections.LEGACY_EXTENSIONS)
    }

    // =========================================================================
    // 6. PHASE 09B-A: DUBBING PROVIDER GROUPS CONTRACT & VALIDATION TESTS
    // =========================================================================

    // =========================================================================
    // 6. PHASE 09B-A: DUBBING PROVIDER GROUPS CONTRACT & VALIDATION TESTS
    // =========================================================================

    @Test
    fun testArabicDubbedSerializationAndAliases() {
        val config = SearchOrderConfig(
            arabicDubbed = listOf("stardima", "spacetoon_go", "anime_dub_ar"),
            updatedAt = 1759200000000L,
            updatedBy = "admin@cinestream.com"
        )
        val map = config.toMap()

        // Verify primary canonical key "arabicDubbed"
        assertEquals(listOf("stardima", "spacetoon_go", "anime_dub_ar"), map["arabicDubbed"])
        // Verify backward-compatibility dual-write aliases
        assertEquals(listOf("stardima", "spacetoon_go", "anime_dub_ar"), map["arabicDub"])
        assertEquals(listOf("stardima", "spacetoon_go", "anime_dub_ar"), map["arabic_dub"])

        // Deserialization with "arabicDubbed" present
        val restoredFromPrimary = SearchOrderConfig.fromMap(map)
        assertEquals(config.arabicDubbed, restoredFromPrimary.arabicDubbed)
        assertEquals(config.arabicDubbed, restoredFromPrimary.arabicDub) // Property alias

        // Deserialization fallback when only legacy "arabicDub" exists
        val legacyDocCamel = mapOf("arabicDub" to listOf("stardima", "spacetoon_go"))
        val restoredFromCamel = SearchOrderConfig.fromMap(legacyDocCamel)
        assertEquals(listOf("stardima", "spacetoon_go"), restoredFromCamel.arabicDubbed)

        // Deserialization fallback when only snake_case "arabic_dub" exists in Firestore doc
        val legacyDocSnake = mapOf("arabic_dub" to listOf("stardima", "spacetoon_go"))
        val restoredFromSnake = SearchOrderConfig.fromMap(legacyDocSnake)
        assertEquals(listOf("stardima", "spacetoon_go"), restoredFromSnake.arabicDubbed)
    }

    @Test
    fun testEnglishDubbedSerializationAndAliases() {
        val config = SearchOrderConfig(
            englishDubbed = listOf("crunchyroll_dub", "hidive_dub"),
            updatedAt = 1759200000000L,
            updatedBy = "admin@cinestream.com"
        )
        val map = config.toMap()

        // Verify primary canonical key "englishDubbed"
        assertEquals(listOf("crunchyroll_dub", "hidive_dub"), map["englishDubbed"])
        // Verify backward-compatibility dual-write aliases
        assertEquals(listOf("crunchyroll_dub", "hidive_dub"), map["englishDub"])
        assertEquals(listOf("crunchyroll_dub", "hidive_dub"), map["english_dub"])

        // Deserialization with "englishDubbed" present
        val restored = SearchOrderConfig.fromMap(map)
        assertEquals(config.englishDubbed, restored.englishDubbed)
        assertEquals(config.englishDubbed, restored.englishDub)

        // Deserialization fallback when only snake_case "english_dub" exists
        val snakeDoc = mapOf("english_dub" to listOf("crunchyroll_dub"))
        val restoredFromSnake = SearchOrderConfig.fromMap(snakeDoc)
        assertEquals(listOf("crunchyroll_dub"), restoredFromSnake.englishDubbed)
    }

    @Test
    fun testDubbingConfigDefaultValuesWhenAbsent() {
        // Document without dubbingConfig or dubbing fields (legacy document)
        val legacyDoc = mapOf(
            "movie" to listOf("egydead"),
            "tv" to listOf("qfilm"),
            "anime" to listOf("witanime")
        )
        val parsed = SearchOrderConfig.fromMap(legacyDoc)

        assertTrue("arabicDubbed must default to empty list", parsed.arabicDubbed.isEmpty())
        assertTrue("englishDubbed must default to empty list", parsed.englishDubbed.isEmpty())
        assertEquals("arabicDubbedStatus must default to ACTIVE", "ACTIVE", parsed.dubbingConfig.arabicDubbedStatus)
        assertEquals("englishDubbedStatus must default to MAINTENANCE", "MAINTENANCE", parsed.dubbingConfig.englishDubbedStatus)
    }

    @Test
    fun testEnglishDubbedStatusDefaultsToMaintenanceInAllMissingCases() {
        // 1. Map with dubbingConfig but missing englishDubbedStatus
        val mapMissingEnglish = mapOf(
            "dubbingConfig" to mapOf("arabicDubbedStatus" to "ACTIVE")
        )
        val parsed1 = SearchOrderConfig.fromMap(mapMissingEnglish)
        assertEquals("MAINTENANCE", parsed1.dubbingConfig.englishDubbedStatus)

        // 2. Map with empty string for englishDubbedStatus
        val mapEmptyEnglish = mapOf(
            "dubbingConfig" to mapOf("arabicDubbedStatus" to "ACTIVE", "englishDubbedStatus" to "")
        )
        val parsed2 = SearchOrderConfig.fromMap(mapEmptyEnglish)
        assertEquals("MAINTENANCE", parsed2.dubbingConfig.englishDubbedStatus)

        // 3. Map with unrecognized status for englishDubbedStatus
        val mapUnknownEnglish = mapOf(
            "dubbingConfig" to mapOf("englishDubbedStatus" to "UNKNOWN_VALUE")
        )
        val parsed3 = SearchOrderConfig.fromMap(mapUnknownEnglish)
        assertEquals("MAINTENANCE", parsed3.dubbingConfig.englishDubbedStatus)
    }

    @Test
    fun testDuplicateIdsRejectedInDubbingGroups() {
        val available = mapOf(
            "ext_ar" to ManagedExtension(
                extensionId = "ext_ar",
                scraperKey = "ext_ar",
                contentTypes = listOf(ManagedExtensionContentTypes.ARABIC_DUB),
                capabilities = listOf(ManagedExtensionCapabilities.SEARCH)
            ),
            "ext_en" to ManagedExtension(
                extensionId = "ext_en",
                scraperKey = "ext_en",
                contentTypes = listOf(ManagedExtensionContentTypes.ENGLISH_DUB),
                capabilities = listOf(ManagedExtensionCapabilities.SEARCH)
            )
        )

        // Duplicates in Arabic Dubbed
        val dupeAr = SearchOrderConfig(arabicDubbed = listOf("ext_ar", "ext_ar"))
        val arErrors = SearchOrderValidator.validate(dupeAr, available)
        assertTrue(arErrors.any { it.contains("Duplicate extension ID in Arabic Dubbed") })

        // Duplicates in English Dubbed
        val dupeEn = SearchOrderConfig(englishDubbed = listOf("ext_en", "ext_en"))
        val enErrors = SearchOrderValidator.validate(dupeEn, available)
        assertTrue(enErrors.any { it.contains("Duplicate extension ID in English Dubbed") })
    }

    @Test
    fun testDubbingCategoryIsolation() {
        val original = SearchOrderConfig(
            movie = listOf("m1"),
            tv = listOf("t1"),
            anime = listOf("a1"),
            arabicDubbed = listOf("ar1", "ar2"),
            englishDubbed = listOf("en1")
        )

        // Update Arabic Dubbed only
        val updatedArabic = original.withOrderForCategory(SearchOrderCategory.ARABIC_DUB, listOf("ar2", "ar1", "ar3"))

        assertEquals(listOf("ar2", "ar1", "ar3"), updatedArabic.arabicDubbed)
        // All other categories MUST remain completely untouched
        assertEquals(listOf("m1"), updatedArabic.movie)
        assertEquals(listOf("t1"), updatedArabic.tv)
        assertEquals(listOf("a1"), updatedArabic.anime)
        assertEquals(listOf("en1"), updatedArabic.englishDubbed)

        // Update English Dubbed only
        val updatedEnglish = original.withOrderForCategory(SearchOrderCategory.ENGLISH_DUB, listOf("en2", "en1"))
        assertEquals(listOf("en2", "en1"), updatedEnglish.englishDubbed)
        assertEquals(listOf("ar1", "ar2"), updatedEnglish.arabicDubbed)
    }

    @Test
    fun testArabicDubbingEligibilityValidation() {
        // ext1 has ARABIC_DUB in audioCapabilities
        val extAudio = ManagedExtension(
            extensionId = "ext_ar_audio",
            scraperKey = "ext_ar_audio",
            audioCapabilities = listOf(com.example.models.ManagedExtensionAudioCapabilities.ARABIC_DUB),
            capabilities = listOf(ManagedExtensionCapabilities.SEARCH)
        )
        // ext2 has ARABIC_DUB in capabilities
        val extCap = ManagedExtension(
            extensionId = "ext_ar_cap",
            scraperKey = "ext_ar_cap",
            capabilities = listOf(ManagedExtensionCapabilities.ARABIC_DUB, ManagedExtensionCapabilities.SEARCH)
        )
        // ext3 has neither
        val extIneligible = ManagedExtension(
            extensionId = "ext_ineligible",
            scraperKey = "ext_ineligible",
            contentTypes = listOf(ManagedExtensionContentTypes.MOVIE),
            capabilities = listOf(ManagedExtensionCapabilities.SEARCH)
        )

        assertTrue(SearchOrderValidator.isExtensionEligibleForCategory(extAudio, SearchOrderCategory.ARABIC_DUB))
        assertTrue(SearchOrderValidator.isExtensionEligibleForCategory(extCap, SearchOrderCategory.ARABIC_DUB))
        assertFalse(SearchOrderValidator.isExtensionEligibleForCategory(extIneligible, SearchOrderCategory.ARABIC_DUB))

        val catalog = mapOf(
            "ext_ar_audio" to extAudio,
            "ext_ar_cap" to extCap,
            "ext_ineligible" to extIneligible
        )

        // Valid configuration
        val validConfig = SearchOrderConfig(arabicDubbed = listOf("ext_ar_audio", "ext_ar_cap"))
        val errorsValid = SearchOrderValidator.validate(validConfig, catalog)
        assertTrue(errorsValid.isEmpty())

        // Invalid configuration (ineligible extension added)
        val invalidConfig = SearchOrderConfig(arabicDubbed = listOf("ext_ar_audio", "ext_ineligible"))
        val errorsInvalid = SearchOrderValidator.validate(invalidConfig, catalog)
        assertFalse(errorsInvalid.isEmpty())
        assertTrue(errorsInvalid.any { it.contains("requires ARABIC_DUB capability") })
    }

    @Test
    fun testEnglishDubbingEligibilityValidation() {
        val extAudio = ManagedExtension(
            extensionId = "ext_en_audio",
            scraperKey = "ext_en_audio",
            audioCapabilities = listOf(com.example.models.ManagedExtensionAudioCapabilities.ENGLISH_DUB),
            capabilities = listOf(ManagedExtensionCapabilities.SEARCH)
        )
        val extIneligible = ManagedExtension(
            extensionId = "ext_anime_only",
            scraperKey = "ext_anime_only",
            contentTypes = listOf(ManagedExtensionContentTypes.ANIME),
            capabilities = listOf(ManagedExtensionCapabilities.SEARCH)
        )

        assertTrue(SearchOrderValidator.isExtensionEligibleForCategory(extAudio, SearchOrderCategory.ENGLISH_DUB))
        assertFalse(SearchOrderValidator.isExtensionEligibleForCategory(extIneligible, SearchOrderCategory.ENGLISH_DUB))

        val catalog = mapOf(
            "ext_en_audio" to extAudio,
            "ext_anime_only" to extIneligible
        )

        val invalidConfig = SearchOrderConfig(englishDubbed = listOf("ext_anime_only"))
        val errors = SearchOrderValidator.validate(invalidConfig, catalog)
        assertFalse(errors.isEmpty())
        assertTrue(errors.any { it.contains("requires ENGLISH_DUB capability") })
    }

    @Test
    fun testExtensionWithoutSearchCapabilityRejected() {
        val noSearchExt = ManagedExtension(
            extensionId = "no_search_ext",
            scraperKey = "no_search",
            capabilities = listOf("DETAILS", "EPISODES"), // Missing SEARCH
            contentTypes = listOf("MOVIE")
        )
        val catalog = mapOf("no_search_ext" to noSearchExt)

        assertFalse(SearchOrderValidator.supportsSearch(noSearchExt))
        assertFalse(SearchOrderValidator.isExtensionEligibleForCategory(noSearchExt, SearchOrderCategory.MOVIE))

        val config = SearchOrderConfig(movie = listOf("no_search_ext"))
        val errors = SearchOrderValidator.validate(config, catalog)
        assertTrue(errors.any { it.contains("does not support SEARCH capability") })
    }

    @Test
    fun testDefaultSearchOrderWithDubbing() {
        val repo = SearchOrderRepository()
        val available = listOf(
            ManagedExtension(extensionId = "dub_ar_high", scraperKey = "ar1", priority = 200, audioCapabilities = listOf("ARABIC_DUB")),
            ManagedExtension(extensionId = "dub_ar_low", scraperKey = "ar2", priority = 90, contentTypes = listOf("ARABIC_DUB")),
            ManagedExtension(extensionId = "dub_en_top", scraperKey = "en1", priority = 150, audioCapabilities = listOf("ENGLISH_DUB")),
            ManagedExtension(extensionId = "movie_top", scraperKey = "m1", priority = 100, contentTypes = listOf("MOVIE"))
        )

        val defaults = repo.getDefaultSearchOrder(available)

        assertEquals(listOf("dub_ar_high", "dub_ar_low"), defaults.arabicDubbed)
        assertEquals(listOf("dub_en_top"), defaults.englishDubbed)
        assertEquals(listOf("movie_top"), defaults.movie)
        assertEquals("MAINTENANCE", defaults.dubbingConfig.englishDubbedStatus)
    }

    @Test
    fun testEnglishDubbingNotAutomaticallyActivated() {
        // Adding items to englishDubbed does NOT make it ACTIVE
        val config = SearchOrderConfig(
            englishDubbed = listOf("en_scraper_1", "en_scraper_2"),
            dubbingConfig = com.example.models.DubbingConfig()
        )
        assertEquals("MAINTENANCE", config.dubbingConfig.englishDubbedStatus)

        // Attempting to set englishStatus to ACTIVE in Phase 09B-A is rejected by validator
        val activeEnConfig = config.copy(
            dubbingConfig = config.dubbingConfig.copy(englishDubbedStatus = "ACTIVE")
        )
        val catalog = mapOf(
            "en_scraper_1" to ManagedExtension(extensionId = "en_scraper_1", scraperKey = "en1", audioCapabilities = listOf("ENGLISH_DUB")),
            "en_scraper_2" to ManagedExtension(extensionId = "en_scraper_2", scraperKey = "en2", audioCapabilities = listOf("ENGLISH_DUB"))
        )
        val errors = SearchOrderValidator.validate(activeEnConfig, catalog)
        assertTrue(errors.any { it.contains("English Dubbing group cannot be activated in Phase 09B-A") })
    }

    @Test
    fun testLanguageFieldPreservedAsCatalogLanguageAndNotConflatedWithAudio() {
        // Invariant: language = "ar" indicates UI/website language, NOT audio dubbing track
        val ext = ManagedExtension(
            extensionId = "foreign_site",
            scraperKey = "foreign",
            language = "ar", // Arabic website metadata
            audioCapabilities = emptyList() // No confirmed Arabic voiceover track
        )

        assertEquals("ar", ext.language)
        assertFalse("Unconfirmed audio capabilities cannot be inferred from site language", ext.hasConfirmedArabicDub)
        assertFalse("Audio capability is unconfirmed", ext.isAudioCapabilityConfirmed)
    }
}

