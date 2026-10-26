package com.example.validation

import com.example.models.DubbingGroupStatus
import com.example.models.ManagedExtension
import com.example.models.ManagedExtensionAudioCapabilities
import com.example.models.ManagedExtensionCapabilities
import com.example.models.ManagedExtensionContentTypes
import com.example.models.SearchOrderCategory
import com.example.models.SearchOrderConfig

/**
 * Domain Validator for Search Ordering by Content Type and Dubbing Group (Phase EXT-CANONICAL-01 & Phase 09B-A).
 *
 * Enforces strict capability checking:
 * 1. Extension IDs must exist in the canonical registry.
 * 2. Extension IDs must be unique within each category list (no duplicates).
 * 3. Extensions must support the SEARCH capability.
 * 4. Extensions must support the specific content type or audio capability of the list they belong to.
 * 5. Arabic Dubbing requires ARABIC_DUB capability.
 * 6. English Dubbing requires ENGLISH_DUB capability.
 * 7. Non-operational (DISABLED / MAINTENANCE) extensions are permitted in order lists (preserves rank).
 * 8. English dubbing group status must remain in MAINTENANCE by default and cannot be automatically activated.
 * 9. Empty lists are permissible (e.g. fresh installation or unconfigured category).
 */
object SearchOrderValidator {

    /**
     * Checks if the extension declares support for the SEARCH operation.
     */
    fun supportsSearch(ext: ManagedExtension): Boolean {
        // If capabilities is empty (legacy format) or explicitly contains SEARCH
        return ext.capabilities.isEmpty() || ext.capabilities.any { it.trim().equals(ManagedExtensionCapabilities.SEARCH, ignoreCase = true) }
    }

    /**
     * Checks whether the extension has declared Arabic dubbing capability.
     */
    fun hasArabicDub(ext: ManagedExtension): Boolean {
        return ext.audioCapabilities.any { it.trim().equals(ManagedExtensionAudioCapabilities.ARABIC_DUB, ignoreCase = true) } ||
               ext.capabilities.any { it.trim().equals(ManagedExtensionAudioCapabilities.ARABIC_DUB, ignoreCase = true) } ||
               ext.contentTypes.any { it.trim().equals(ManagedExtensionAudioCapabilities.ARABIC_DUB, ignoreCase = true) }
    }

    /**
     * Checks whether the extension has declared English dubbing capability.
     */
    fun hasEnglishDub(ext: ManagedExtension): Boolean {
        return ext.audioCapabilities.any { it.trim().equals(ManagedExtensionAudioCapabilities.ENGLISH_DUB, ignoreCase = true) } ||
               ext.capabilities.any { it.trim().equals(ManagedExtensionAudioCapabilities.ENGLISH_DUB, ignoreCase = true) } ||
               ext.contentTypes.any { it.trim().equals(ManagedExtensionAudioCapabilities.ENGLISH_DUB, ignoreCase = true) }
    }

    /**
     * Verifies whether audio capabilities are confirmed via audioCapabilities field
     * rather than inferred from legacy fields or site language.
     */
    fun isAudioCapabilityConfirmed(ext: ManagedExtension, category: SearchOrderCategory): Boolean {
        return when (category) {
            SearchOrderCategory.ARABIC_DUB -> ext.hasConfirmedArabicDub
            SearchOrderCategory.ENGLISH_DUB -> ext.hasConfirmedEnglishDub
            else -> true
        }
    }

    /**
     * Determine if a [ManagedExtension] is functionally eligible for a [SearchOrderCategory].
     */
    fun isExtensionEligibleForCategory(ext: ManagedExtension, category: SearchOrderCategory): Boolean {
        if (!supportsSearch(ext)) return false

        val types = ext.contentTypes.map { it.trim().uppercase() }
        return when (category) {
            SearchOrderCategory.MOVIE -> {
                types.contains(ManagedExtensionContentTypes.MOVIE) || types.contains("MOVIES")
            }
            SearchOrderCategory.TV -> {
                types.contains(ManagedExtensionContentTypes.SERIES) ||
                types.contains("TV") ||
                types.contains("SERIES") ||
                types.contains("SHOWS")
            }
            SearchOrderCategory.ANIME -> {
                types.contains(ManagedExtensionContentTypes.ANIME)
            }
            SearchOrderCategory.ARABIC_DUB -> {
                hasArabicDub(ext)
            }
            SearchOrderCategory.ENGLISH_DUB -> {
                hasEnglishDub(ext)
            }
        }
    }

    /**
     * Validate a complete [SearchOrderConfig] against the provided catalog of available extensions.
     *
     * Returns a list of error strings. If the list is empty, the configuration is valid.
     */
    fun validate(
        config: SearchOrderConfig,
        availableExtensions: Map<String, ManagedExtension>
    ): List<String> {
        val errors = mutableListOf<String>()

        validateCategoryList(
            category = SearchOrderCategory.MOVIE,
            ids = config.movie,
            availableExtensions = availableExtensions,
            errors = errors
        )

        validateCategoryList(
            category = SearchOrderCategory.TV,
            ids = config.tv,
            availableExtensions = availableExtensions,
            errors = errors
        )

        validateCategoryList(
            category = SearchOrderCategory.ANIME,
            ids = config.anime,
            availableExtensions = availableExtensions,
            errors = errors
        )

        validateCategoryList(
            category = SearchOrderCategory.ARABIC_DUB,
            ids = config.arabicDubbed,
            availableExtensions = availableExtensions,
            errors = errors
        )

        validateCategoryList(
            category = SearchOrderCategory.ENGLISH_DUB,
            ids = config.englishDubbed,
            availableExtensions = availableExtensions,
            errors = errors
        )

        // Invariant: English Dubbing status must remain in MAINTENANCE mode by default
        if (config.dubbingConfig.englishStatusEnum == DubbingGroupStatus.ACTIVE) {
            errors.add("English Dubbing group cannot be activated in Phase 09B-A (must remain in MAINTENANCE status)")
        }

        return errors
    }

    private fun validateCategoryList(
        category: SearchOrderCategory,
        ids: List<String>,
        availableExtensions: Map<String, ManagedExtension>,
        errors: MutableList<String>
    ) {
        val categoryLabel = category.labelEn

        // 1. Check for duplicates
        val seen = mutableSetOf<String>()
        val duplicates = mutableSetOf<String>()
        for (id in ids) {
            val normalized = id.trim()
            if (!seen.add(normalized)) {
                duplicates.add(normalized)
            }
        }
        if (duplicates.isNotEmpty()) {
            errors.add("Duplicate extension ID in $categoryLabel search order: ${duplicates.joinToString()}")
        }

        // 2. Validate existence, scraperKey, and capability compliance
        for (id in ids) {
            val normalized = id.trim()
            val ext = availableExtensions[normalized]
            if (ext == null) {
                errors.add("Extension '$normalized' in $categoryLabel search order does not exist in registry")
                continue
            }

            val effectiveKey = ext.scraperKey.ifBlank { ext.extensionId }
            if (effectiveKey.isBlank()) {
                errors.add("Extension '$normalized' in $categoryLabel search order has missing or empty scraperKey")
            }

            if (!supportsSearch(ext)) {
                val displayName = ext.name.ifBlank { normalized }
                errors.add("Extension '$displayName' ($normalized) does not support SEARCH capability")
                continue
            }

            if (!isExtensionEligibleForCategory(ext, category)) {
                val displayName = ext.name.ifBlank { normalized }
                when (category) {
                    SearchOrderCategory.ARABIC_DUB -> {
                        errors.add("Extension '$displayName' ($normalized) requires ARABIC_DUB capability")
                    }
                    SearchOrderCategory.ENGLISH_DUB -> {
                        errors.add("Extension '$displayName' ($normalized) requires ENGLISH_DUB capability")
                    }
                    else -> {
                        val supported = if (ext.contentTypes.isEmpty()) "NONE" else ext.contentTypes.joinToString()
                        errors.add("Extension '$displayName' ($normalized) does not support content type $categoryLabel (Supported: $supported)")
                    }
                }
            }
        }
    }
}
