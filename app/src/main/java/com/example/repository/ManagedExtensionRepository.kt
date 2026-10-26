package com.example.repository

import com.example.contract.FirebaseCollections
import com.example.contract.FirebaseConfigDocs
import com.example.diagnostics.AppLogger
import com.example.models.ManagedExtension
import com.example.models.ManagedExtensionStatus
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

/**
 * Default catalog mirrors the original bundled Users App configuration.
 * The immutable raw representation is defined in [OriginalUserAppExtensionDefaults].
 */
object DefaultCineStreamScrapers {
    fun getDefaults(): List<ManagedExtension> = OriginalUserAppExtensionDefaults.getDefaults()
}

/**
 * Authoritative Repository for Modern Managed Extensions (Phase C2).
 * Dual Source & Dual Write:
 * - /managed_extensions/{extensionId}
 * - /extensions/{extensionId}
 * Invariant: Pure configuration datastore. Zero executable code.
 */
class ManagedExtensionRepository(
    private val adminRepository: AdminRepository = AdminRepository()
) {
    private fun ensureFirebase() {
        try {
            val apps = com.google.firebase.FirebaseApp.getApps(com.example.MyApplication.instance)
            if (apps.isEmpty()) {
                com.example.diagnostics.FirebaseInitializer.init(com.example.MyApplication.instance)
            }
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "ensureFirebase check notice: ${e.message}")
        }
    }

    private val firestore: FirebaseFirestore by lazy {
        ensureFirebase()
        FirebaseFirestore.getInstance()
    }

    private val managedCollection get() = firestore.collection(FirebaseCollections.MANAGED_EXTENSIONS)
    private val legacyCollection get() = firestore.collection(FirebaseCollections.LEGACY_EXTENSIONS)
    private val sourceBaselineCollection get() = firestore.collection(FirebaseCollections.EXTENSION_SOURCE_BASELINE)

    // Templates are derived from the current bundled scraper routes. These are Admin catalog
    // defaults, not part of the immutable raw Users App baseline.
    private val defaultSearchUrlPaths = mapOf(
        "qfilm" to "/search.php?keywords={query}",
        "anime4up" to "/{pagePath}?s={query}",
        "animeblkom" to "/search?query={query}",
        "egydead" to "/{pagePath}?s={query}",
        "witanime" to "/search?q={query}"
    )

    private fun applyDefaultSearchUrlWhenMissing(
        extensionId: String,
        payload: Map<String, Any>
    ): Map<String, Any> {
        val existing = (payload["searchUrl"] as? String)?.trim().orEmpty()
        if (existing.isNotBlank()) return payload
        val path = defaultSearchUrlPaths[extensionId.lowercase()] ?: return payload
        val baseUrl = (payload["baseUrl"] as? String)?.trim()?.trimEnd('/').orEmpty()
        // Build from the current Admin baseUrl so a prior domain rotation is preserved.
        if (!baseUrl.startsWith("https://", ignoreCase = true)) return payload
        return payload.toMutableMap().apply { put("searchUrl", baseUrl + path) }
    }

    private data class OriginalExtensionBaselineRecord(
        val extensionId: String,
        val rawPayload: Map<String, Any>
    )

    /**
     * Reads the current source baseline from the server. It must never silently fall back to
     * hardcoded Admin defaults: that would make "Restore Original" restore the wrong source.
     */
    private suspend fun loadOriginalUserAppBaseline(): List<OriginalExtensionBaselineRecord> {
        val expectedIds = OriginalUserAppExtensionDefaults.getDefaults().map { it.extensionId.lowercase() }
        val expectedKeys = setOf(
            "id", "extensionId", "scraperKey", "name", "description", "baseUrl", "iconUrl",
            "runtimeApiVersion", "definitionVersion", "minAppVersionCode", "status", "priority",
            "language", "contentTypes", "audioCapabilities", "enabled", "updatedAt"
        )
        val allowedMetadataKeys = setOf("sourceHash", "sourceSchemaVersion")
        val snapshot = sourceBaselineCollection.get(Source.SERVER).await()
        val documentsById = snapshot.documents.associateBy { it.id.lowercase() }
        val missingIds = expectedIds.filter { documentsById[it] == null }
        if (missingIds.isNotEmpty()) {
            throw IllegalStateException(
                "Original Users App baseline is incomplete in /${FirebaseCollections.EXTENSION_SOURCE_BASELINE}: " +
                    "missing ${missingIds.joinToString()}. Open the updated Users App while signed in and online, " +
                    "then retry after its first-upload baseline has completed. No catalog changes were applied."
            )
        }

        return expectedIds.map { id ->
            val doc = documentsById[id]
                ?: throw IllegalStateException("Missing original Users App baseline document: $id")
            if (doc.id != id) {
                throw IllegalStateException("Baseline document ID must be canonical lowercase '$id'; found '${doc.id}'.")
            }
            val data = doc.data
                ?: throw IllegalStateException("Baseline document '$id' has no data.")
            val missing = expectedKeys - data.keys
            val extra = data.keys - expectedKeys - allowedMetadataKeys
            if (missing.isNotEmpty() || extra.isNotEmpty()) {
                throw IllegalStateException(
                    "Baseline document '$id' has an invalid raw schema (missing=${missing.joinToString()}, extra=${extra.joinToString()})."
                )
            }
            if (data["sourceHash"] != null && data["sourceHash"] !is String) {
                throw IllegalStateException("Baseline document '$id' has an invalid sourceHash metadata field.")
            }
            if (data["sourceSchemaVersion"] != null && data["sourceSchemaVersion"] !is Number) {
                throw IllegalStateException("Baseline document '$id' has an invalid sourceSchemaVersion metadata field.")
            }
            // Hash/version metadata is not part of the raw source definition and must never
            // leak into active extension documents during Sync or Restore.
            val rawData = data.filterKeys { it in expectedKeys }
            if (data["id"] != id || data["extensionId"] != id || data["scraperKey"] != id) {
                throw IllegalStateException("Baseline document '$id' contains mismatched source identity fields.")
            }
            val baseUrl = data["baseUrl"] as? String
            if (baseUrl.isNullOrBlank() || !(baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))) {
                throw IllegalStateException("Baseline document '$id' contains an invalid baseUrl.")
            }
            if (data["name"] !is String || data["description"] !is String || data["iconUrl"] !is String ||
                data["language"] !is String || data["status"] !is String || data["enabled"] !is Boolean ||
                data["priority"] !is Number || data["runtimeApiVersion"] !is Number ||
                data["definitionVersion"] !is Number || data["minAppVersionCode"] !is Number ||
                data["updatedAt"] !is Number || data["contentTypes"] !is List<*> ||
                data["audioCapabilities"] !is List<*>
            ) {
                throw IllegalStateException("Baseline document '$id' failed raw-field validation.")
            }
            OriginalExtensionBaselineRecord(id, rawData.toMap())
        }
    }

    private fun parseDocument(doc: DocumentSnapshot): ManagedExtension? {
        try {
            val docId = doc.id
            if (docId.isBlank()) return null

            val id = doc.getString("extensionId")
                ?: doc.getString("id")
                ?: doc.getString("key")
                ?: docId

            val scraperKey = doc.getString("scraperKey")
                ?: doc.getString("key")
                ?: doc.getString("scraper")
                ?: docId.trim().lowercase()

            val name = doc.getString("name")
                ?: doc.getString("title")
                ?: doc.getString("label")
                ?: docId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            val baseUrl = doc.getString("baseUrl")
                ?: doc.getString("url")
                ?: doc.getString("siteUrl")
                ?: doc.getString("domain")
                ?: ""

            val searchUrl = doc.getString("searchUrl") ?: doc.getString("search_url") ?: ""

            val rawPriority = doc.get("priority")
            val priority = when (rawPriority) {
                is Number -> rawPriority.toInt()
                is String -> rawPriority.toIntOrNull() ?: 100
                else -> 100
            }

            val rawMinAppVersion = doc.get("minAppVersionCode") ?: doc.get("minVersionCode")
            val minAppVersionCode = when (rawMinAppVersion) {
                is Number -> rawMinAppVersion.toInt()
                is String -> rawMinAppVersion.toIntOrNull() ?: 1
                else -> 1
            }

            val rawEnabled = doc.get("enabled")
            val rawStatus = doc.getString("status")

            val enabled = when (rawEnabled) {
                is Boolean -> rawEnabled
                is String -> rawEnabled.equals("true", ignoreCase = true) || rawEnabled.equals("active", ignoreCase = true)
                is Number -> rawEnabled.toInt() != 0
                else -> {
                    if (!rawStatus.isNullOrBlank()) {
                        rawStatus.equals("ACTIVE", ignoreCase = true) || rawStatus.equals("ENABLED", ignoreCase = true)
                    } else true
                }
            }

            val safeStatus = if (!rawStatus.isNullOrBlank()) {
                ManagedExtensionStatus.fromString(rawStatus).name
            } else {
                if (enabled) ManagedExtensionStatus.ACTIVE.name else ManagedExtensionStatus.DISABLED.name
            }

            val rawCaps = doc.get("capabilities")
            val caps = when (rawCaps) {
                is List<*> -> rawCaps.mapNotNull { it?.toString()?.trim()?.uppercase() }
                is String -> rawCaps.split(",").map { it.trim().uppercase() }
                else -> listOf("SEARCH", "DETAILS", "EPISODES", "SERVER_DISCOVERY", "VIDEO_EXTRACTION")
            }

            val rawTypes = doc.get("contentTypes")
            val types = when (rawTypes) {
                is List<*> -> rawTypes.mapNotNull { it?.toString()?.trim()?.uppercase() }
                is String -> rawTypes.split(",").map { it.trim().uppercase() }
                else -> listOf("MOVIE", "SERIES")
            }

            val rawAudioCaps = doc.get("audioCapabilities")
            val audioCaps = when (rawAudioCaps) {
                is List<*> -> rawAudioCaps.mapNotNull { it?.toString()?.trim()?.uppercase() }
                is String -> rawAudioCaps.split(",").map { it.trim().uppercase() }
                else -> emptyList()
            }

            val iconUrl = doc.getString("iconUrl") ?: doc.getString("icon") ?: ""
            val language = doc.getString("language") ?: doc.getString("lang") ?: "ar"

            val description = doc.getString("description") ?: doc.getString("desc") ?: ""
            val runtimeApiVersion = (doc.get("runtimeApiVersion") as? Number)?.toInt() ?: 1
            val definitionVersion = (doc.get("definitionVersion") as? Number)?.toInt() ?: 1
            val createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
            val updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: System.currentTimeMillis()

            return ManagedExtension(
                extensionId = id,
                scraperKey = scraperKey,
                name = name,
                description = description,
                baseUrl = baseUrl,
                searchUrl = searchUrl,
                iconUrl = iconUrl,
                runtimeApiVersion = runtimeApiVersion,
                definitionVersion = definitionVersion,
                minAppVersionCode = minAppVersionCode,
                status = safeStatus,
                priority = priority,
                language = language,
                capabilities = caps,
                contentTypes = types,
                audioCapabilities = audioCaps,
                enabled = enabled && safeStatus == ManagedExtensionStatus.ACTIVE.name,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "Failed to deserialize doc ${doc.id}: ${e.message}")
            return null
        }
    }

    private fun listenCollection(col: CollectionReference, name: String): Flow<Map<String, ManagedExtension>> = callbackFlow {
        val listener = col.addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                AppLogger.w("ManagedExtensionRepo", "Listener notice for $name: ${error.message}")
                trySend(emptyMap())
                if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    close(error)
                }
                return@addSnapshotListener
            }
            val map = mutableMapOf<String, ManagedExtension>()
            snapshot?.documents?.forEach { doc ->
                parseDocument(doc)?.let { ext ->
                    map[ext.extensionId] = ext
                }
            }
            trySend(map)
        }
        awaitClose { listener.remove() }
    }.onStart {
        emit(emptyMap())
    }.retryWhen { cause, attempt ->
        delay(minOf(1000L * (attempt + 1), 5000L))
        true
    }

    fun getAllManagedExtensions(): Flow<List<ManagedExtension>> {
        return combine(
            listenCollection(managedCollection, "managed_extensions"),
            listenCollection(legacyCollection, "extensions")
        ) { managedMap, legacyMap ->
            // Merge maps: start with legacy, overlay managed_extensions
            val mergedMap = HashMap<String, ManagedExtension>(legacyMap)
            managedMap.forEach { (id, ext) ->
                mergedMap[id] = ext
            }
            // Return authentic persistent data without auto-resurrecting deleted extensions
            mergedMap.values.sortedByDescending { it.priority }
        }
    }

    suspend fun forceRefresh() {
        try {
            managedCollection.get(com.google.firebase.firestore.Source.SERVER).await()
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "forceRefresh managed notice: ${e.message}")
        }
        try {
            legacyCollection.get(com.google.firebase.firestore.Source.SERVER).await()
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "forceRefresh legacy notice: ${e.message}")
        }
    }

    suspend fun getManagedExtension(id: String): ManagedExtension? {
        val doc1 = try {
            managedCollection.document(id).get().await()
        } catch (e: Exception) {
            null
        }
        if (doc1 != null && doc1.exists()) {
            return parseDocument(doc1)
        }
        val doc2 = try {
            legacyCollection.document(id).get().await()
        } catch (e: Exception) {
            null
        }
        if (doc2 != null && doc2.exists()) {
            return parseDocument(doc2)
        }
        return null
    }

    suspend fun saveManagedExtension(extension: ManagedExtension, isNew: Boolean) {
        val now = System.currentTimeMillis()
        val id = extension.extensionId.trim().ifBlank {
            extension.scraperKey.trim().ifBlank { "ext_${System.currentTimeMillis()}" }
        }
        val isEnabled = extension.enabled && extension.status.trim().uppercase() == "ACTIVE"

        val payload = hashMapOf<String, Any?>(
            "extensionId" to id,
            "id" to id,
            "scraperKey" to extension.scraperKey.trim().lowercase().ifBlank { id.lowercase() },
            "name" to extension.name.trim(),
            "description" to extension.description.trim(),
            "baseUrl" to extension.baseUrl.trim(),
            "searchUrl" to extension.searchUrl.trim(),
            "iconUrl" to extension.iconUrl.trim(),
            "runtimeApiVersion" to extension.runtimeApiVersion,
            "definitionVersion" to extension.definitionVersion,
            "minAppVersionCode" to extension.minAppVersionCode,
            "status" to extension.status.trim().uppercase(),
            "priority" to extension.priority,
            "language" to extension.language.trim().ifBlank { "ar" },
            "capabilities" to extension.capabilities,
            "contentTypes" to extension.contentTypes,
            "audioCapabilities" to extension.audioCapabilities,
            "enabled" to isEnabled,
            "updatedAt" to now
        )
        if (isNew || extension.createdAt == 0L) {
            payload["createdAt"] = now
        }

        // Dual-write to BOTH collections
        managedCollection.document(id).set(payload, SetOptions.merge()).await()
        try {
            legacyCollection.document(id).set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "Dual write to legacy extensions notice: ${e.message}")
        }

        val action = if (isNew) "CREATE_MANAGED_EXTENSION" else "UPDATE_MANAGED_EXTENSION"
        adminRepository.logAudit(
            action = action,
            targetType = "EXTENSION",
            targetId = id,
            details = "$action: ${extension.name} (baseUrl=${extension.baseUrl}, priority=${extension.priority}, enabled=$isEnabled)"
        )
    }

    suspend fun updateStatus(id: String, status: ManagedExtensionStatus) {
        val now = System.currentTimeMillis()
        val enabled = status == ManagedExtensionStatus.ACTIVE
        val updates = mapOf(
            "status" to status.name,
            "enabled" to enabled,
            "updatedAt" to now
        )
        managedCollection.document(id).set(updates, SetOptions.merge()).await()
        try {
            legacyCollection.document(id).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "Dual write updateStatus notice: ${e.message}")
        }
        adminRepository.logAudit(
            action = "CHANGE_MANAGED_EXTENSION_STATUS",
            targetType = "EXTENSION",
            targetId = id,
            details = "Status changed to ${status.name} (enabled=$enabled)"
        )
    }

    suspend fun updatePriority(id: String, priority: Int) {
        val now = System.currentTimeMillis()
        val updates = mapOf(
            "priority" to priority,
            "updatedAt" to now
        )
        managedCollection.document(id).set(updates, SetOptions.merge()).await()
        try {
            legacyCollection.document(id).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "Dual write updatePriority notice: ${e.message}")
        }
        adminRepository.logAudit(
            action = "CHANGE_MANAGED_EXTENSION_PRIORITY",
            targetType = "EXTENSION",
            targetId = id,
            details = "Priority changed to $priority"
        )
    }

    suspend fun updateBaseUrl(id: String, newBaseUrl: String) {
        val now = System.currentTimeMillis()
        val updates = mapOf(
            "baseUrl" to newBaseUrl.trim(),
            "updatedAt" to now
        )
        managedCollection.document(id).set(updates, SetOptions.merge()).await()
        try {
            legacyCollection.document(id).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "Dual write updateBaseUrl notice: ${e.message}")
        }
        adminRepository.logAudit(
            action = "CHANGE_MANAGED_EXTENSION_BASE_URL",
            targetType = "EXTENSION",
            targetId = id,
            details = "Base URL rotated to: $newBaseUrl"
        )
    }

    suspend fun deleteManagedExtension(id: String) {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return

        // 1. Delete direct document in /managed_extensions
        try {
            managedCollection.document(cleanId).delete().await()
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "Direct managed delete notice: ${e.message}")
        }

        // 2. Delete any matching documents by extensionId or scraperKey in /managed_extensions
        try {
            val q1 = managedCollection.whereEqualTo("extensionId", cleanId).get().await()
            for (doc in q1.documents) { doc.reference.delete() }
            val q2 = managedCollection.whereEqualTo("scraperKey", cleanId).get().await()
            for (doc in q2.documents) { doc.reference.delete() }
        } catch (e: Exception) {
            // non-fatal
        }

        // 3. Delete direct document in /extensions (legacy)
        try {
            legacyCollection.document(cleanId).delete().await()
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "Direct legacy delete notice: ${e.message}")
        }

        // 4. Delete any matching documents by extensionId or scraperKey in /extensions
        try {
            val q3 = legacyCollection.whereEqualTo("extensionId", cleanId).get().await()
            for (doc in q3.documents) { doc.reference.delete() }
            val q4 = legacyCollection.whereEqualTo("scraperKey", cleanId).get().await()
            for (doc in q4.documents) { doc.reference.delete() }
        } catch (e: Exception) {
            // non-fatal
        }

        // 5. Delete from extension_updates
        try {
            firestore.collection(FirebaseCollections.LEGACY_EXTENSION_UPDATES).document(cleanId).delete().await()
        } catch (e: Exception) {
            // non-fatal
        }

        // 6. Clean up /config/search_order
        try {
            val searchOrderDoc = firestore.collection(FirebaseCollections.CONFIG).document(FirebaseConfigDocs.SEARCH_ORDER)
            val snap = searchOrderDoc.get().await()
            if (snap != null && snap.exists()) {
                val movie = (snap.get("movie") as? List<*>)?.mapNotNull { it?.toString() }?.filterNot { it.equals(cleanId, ignoreCase = true) } ?: emptyList()
                val tv = (snap.get("tv") as? List<*>)?.mapNotNull { it?.toString() }?.filterNot { it.equals(cleanId, ignoreCase = true) } ?: emptyList()
                val anime = (snap.get("anime") as? List<*>)?.mapNotNull { it?.toString() }?.filterNot { it.equals(cleanId, ignoreCase = true) } ?: emptyList()
                searchOrderDoc.set(
                    mapOf(
                        "movie" to movie,
                        "tv" to tv,
                        "anime" to anime,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            }
        } catch (e: Exception) {
            AppLogger.w("ManagedExtensionRepo", "Search order clean notice: ${e.message}")
        }

        adminRepository.logAudit(
            action = "DELETE_MANAGED_EXTENSION",
            targetType = "EXTENSION",
            targetId = cleanId,
            details = "Permanently deleted managed extension: $cleanId"
        )
    }

    /**
     * Reconciles only records that belong to the original bundled source IDs. The canonical
     * Admin catalog wins every conflict; /extensions is repaired as its legacy mirror. If both
     * catalogs lack a source, it is initialized from the current Users App source baseline.
     * Custom extension records outside these source IDs are never deleted by Sync.
     */
    suspend fun seedDefaultExtensions(): Int {
        val baseline = loadOriginalUserAppBaseline()
        val canonicalSnapshot = managedCollection.get(Source.SERVER).await()
        val legacySnapshot = legacyCollection.get(Source.SERVER).await()
        val batch = firestore.batch()
        var operations = 0
        var conflictingSources = 0
        val conflictSummaries = mutableListOf<String>()

        fun matches(doc: DocumentSnapshot, id: String): Boolean =
            doc.id.equals(id, ignoreCase = true) ||
                doc.getString("extensionId")?.equals(id, ignoreCase = true) == true ||
                doc.getString("id")?.equals(id, ignoreCase = true) == true ||
                doc.getString("scraperKey")?.equals(id, ignoreCase = true) == true ||
                doc.getString("key")?.equals(id, ignoreCase = true) == true

        for (source in baseline) {
            val id = source.extensionId
            val canonicalMatches = canonicalSnapshot.documents.filter { matches(it, id) }
            val legacyMatches = legacySnapshot.documents.filter { matches(it, id) }
            val canonicalSource = canonicalMatches.firstOrNull { it.id == id }
                ?: canonicalMatches.firstOrNull()
            val legacySource = legacyMatches.firstOrNull { it.id == id }
                ?: legacyMatches.firstOrNull()

            // Conflict policy: canonical Admin data wins, then existing Admin legacy data,
            // and the current Users App source baseline is used only when both are absent.
            val adminHasRecord = canonicalSource != null || legacySource != null
            val chosenAdminPayload: Map<String, Any> = canonicalSource?.data?.toMap()
                ?: legacySource?.data?.toMap()
                ?: source.rawPayload
            // Fill a missing Search URL for the five bundled scrapers without replacing a
            // non-empty Admin-edited template. This repairs the blank-field issue precisely.
            val authoritativePayload = applyDefaultSearchUrlWhenMissing(id, chosenAdminPayload)

            if (adminHasRecord) {
                val baselineDifferences = (source.rawPayload.keys + authoritativePayload.keys)
                    .distinct()
                    .filter { source.rawPayload[it] != authoritativePayload[it] }
                val canonicalData = canonicalSource?.data.orEmpty()
                val legacyData = legacySource?.data.orEmpty()
                val catalogDifferences = if (canonicalSource != null && legacySource != null) {
                    (canonicalData.keys + legacyData.keys).distinct()
                        .filter { canonicalData[it] != legacyData[it] }
                } else emptyList()
                val differingFields = (baselineDifferences + catalogDifferences).distinct().sorted()
                if (differingFields.isNotEmpty()) {
                    conflictingSources++
                    conflictSummaries.add("$id:${differingFields.joinToString(",")}")
                }
            }

            // Full-document replacement removes stale/conflicting fields only from this source.
            if (canonicalSource == null || canonicalSource.id != id || canonicalSource.data != authoritativePayload) {
                batch.set(managedCollection.document(id), authoritativePayload)
                operations++
            }
            canonicalMatches.filterNot { it.id == id }.forEach {
                batch.delete(it.reference)
                operations++
            }

            // The legacy collection mirrors the selected Admin-authoritative payload. When it
            // differs, replace that one source document; unrelated source documents remain intact.
            if (legacySource == null || legacySource.id != id || legacySource.data != authoritativePayload) {
                batch.set(legacyCollection.document(id), authoritativePayload)
                operations++
            }
            legacyMatches.filterNot { it.id == id }.forEach {
                batch.delete(it.reference)
                operations++
            }
        }

        if (operations > 500) {
            throw IllegalStateException(
                "Extension sync requires $operations Firestore writes; the 500-write batch limit would be exceeded. No changes were applied."
            )
        }
        if (operations > 0) {
            batch.commit().await()
        }

        adminRepository.logAudit(
            action = "SYNC_DEFAULT_MANAGED_EXTENSIONS",
            targetType = "EXTENSION_CATALOG",
            targetId = "managed_extensions",
            details = "Compared ${baseline.size} current Users App source baseline records; Admin data won $conflictingSources source conflicts. " +
                "Repaired only matching canonical/legacy records and duplicates. Writes=$operations; fieldDifferences=${conflictSummaries.joinToString("; ").take(3000)}"
        )
        return baseline.size
    }

    /**
     * Destructively restore the exact raw Users App extension configuration only.
     * Replaces both canonical and legacy catalogs and removes non-original sources.
     * Search-order configuration is deliberately outside this operation and is never read or written here.
     */
    suspend fun restoreOriginalSettings(): OriginalExtensionRestoreResult {
        val baseline = loadOriginalUserAppBaseline()
        val originalIds = baseline.map { it.extensionId }.toSet()

        // All reads are from SERVER; local cache or hardcoded Admin defaults must never define restore.
        val canonicalSnapshot = managedCollection.get(Source.SERVER).await()
        val legacySnapshot = legacyCollection.get(Source.SERVER).await()
        // Delete all non-canonical paths (including case variants/duplicates) inside these two
        // extension catalogs, then restore the exact five original raw records at canonical IDs.
        val canonicalToDelete = canonicalSnapshot.documents.filter { it.id !in originalIds }
        val legacyToDelete = legacySnapshot.documents.filter { it.id !in originalIds }
        val plannedWrites = baseline.size * 2 + canonicalToDelete.size + legacyToDelete.size
        if (plannedWrites > 500) {
            throw IllegalStateException(
                "Restore requires $plannedWrites Firestore writes; the 500-write limit would be exceeded. No changes were applied."
            )
        }

        val batch = firestore.batch()
        baseline.forEach { source ->
            // Full replacement intentionally removes Admin edits and any fields absent from the
            // current Users App source baseline. It never writes /config/search_order.
            batch.set(managedCollection.document(source.extensionId), source.rawPayload)
            batch.set(legacyCollection.document(source.extensionId), source.rawPayload)
        }
        canonicalToDelete.forEach { batch.delete(it.reference) }
        legacyToDelete.forEach { batch.delete(it.reference) }
        batch.commit().await()

        adminRepository.logAudit(
            action = "RESTORE_ORIGINAL_MANAGED_EXTENSIONS",
            targetType = "EXTENSION_CATALOG",
            targetId = "managed_extensions",
            details = "Restored ${baseline.size} exact raw definitions from the current Users App source baseline; " +
                "removed ${canonicalToDelete.size} non-canonical and ${legacyToDelete.size} legacy non-canonical records. " +
                "Search-order configuration and source-baseline documents were not modified."
        )

        return OriginalExtensionRestoreResult(
            restoredSources = baseline.size,
            removedDocuments = canonicalToDelete.size + legacyToDelete.size
        )
    }
}

data class OriginalExtensionRestoreResult(
    val restoredSources: Int,
    val removedDocuments: Int
)
