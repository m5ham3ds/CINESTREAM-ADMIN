package com.example

import com.example.repository.DefaultCineStreamScrapers
import com.example.repository.OriginalUserAppExtensionDefaults
import com.example.repository.SearchOrderRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for keeping Admin's restore target identical to the Users App baseline. */
class OriginalUserAppExtensionDefaultsTest {

    @Test
    fun restoreBaselineContainsExactlyTheFiveBundledUsersAppSources() {
        val defaults = OriginalUserAppExtensionDefaults.getDefaults()
        assertEquals(
            listOf("qfilm", "egydead", "witanime", "anime4up", "animeblkom"),
            defaults.map { it.extensionId }
        )
        assertEquals(
            defaults.map { it.extensionId }.toSet(),
            OriginalUserAppExtensionDefaults.canonicalIds
        )
    }

    @Test
    fun rawFirestorePayloadOmitsAdminOnlyOverridesAndUsesBundledValues() {
        val defaults = OriginalUserAppExtensionDefaults.getDefaults().associateBy { it.extensionId }
        val qfilm = defaults.getValue("qfilm")
        val payload = OriginalUserAppExtensionDefaults.toRawFirestorePayload(qfilm)

        assertEquals("qfilm", payload["id"])
        assertEquals("qfilm", payload["extensionId"])
        assertEquals("https://a.qfilm.tv", payload["baseUrl"])
        assertEquals(listOf("MOVIE", "ANIME"), payload["contentTypes"])
        assertEquals(110, (payload["priority"] as Number).toInt())
        assertEquals(OriginalUserAppExtensionDefaults.BASELINE_UPDATED_AT, (payload["updatedAt"] as Number).toLong())
        assertFalse("Raw Users App baseline must not freeze an Admin searchUrl override", payload.containsKey("searchUrl"))
        assertFalse("Raw Users App baseline must not freeze an Admin capabilities override", payload.containsKey("capabilities"))
        assertFalse("createdAt is not part of the bundled raw definition", payload.containsKey("createdAt"))
    }

    @Test
    fun syncDefaultsExposeSameBaselineWithoutTheOldAdminOnlySources() {
        val defaults = DefaultCineStreamScrapers.getDefaults()
        assertEquals(OriginalUserAppExtensionDefaults.canonicalIds, defaults.map { it.extensionId }.toSet())
        assertFalse(defaults.any { it.extensionId in setOf("akwam", "arabseed", "mycima") })
        assertNotNull(defaults.firstOrNull { it.extensionId == "anime4up" })
        assertNotNull(defaults.firstOrNull { it.extensionId == "animeblkom" })
    }

    @Test
    fun defaultSearchOrderIsDerivedFromOriginalTypesAndPriorities() {
        val config = SearchOrderRepository().getDefaultSearchOrder(OriginalUserAppExtensionDefaults.getDefaults())

        assertEquals(listOf("qfilm", "egydead"), config.movie)
        assertEquals(listOf("egydead"), config.tv)
        assertEquals(listOf("qfilm", "anime4up", "animeblkom", "witanime", "egydead"), config.anime)
        assertEquals(listOf("qfilm", "egydead"), config.arabicDubbed)
        assertTrue(config.englishDubbed.isEmpty())
    }
}
