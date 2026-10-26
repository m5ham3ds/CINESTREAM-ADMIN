package com.example.models

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Canonical lifecycle states for Pro Upgrade Requests (Phase C4).
 * Supported: PENDING, APPROVED, REJECTED.
 *
 * Security Invariant: Unknown/malformed status safely resolves to PENDING or REJECTED,
 * NEVER silently interpreted as APPROVED.
 */
enum class ProRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    companion object {
        fun fromString(value: String?): ProRequestStatus {
            return when (value?.trim()?.uppercase()) {
                "PENDING" -> PENDING
                "APPROVED" -> APPROVED
                "REJECTED" -> REJECTED
                "CANCELLED", "CANCELED" -> CANCELLED
                else -> PENDING // Safe fallback: unknown status is treated as PENDING/review, NEVER APPROVED
            }
        }
    }
}

/**
 * Pro Upgrade Request Model.
 * Canonical Document Path: /pro_requests/{requestId}
 * Fully reconciles User App schema (planId, durationDays, paymentReference, notes)
 * with Admin Dashboard schema (requestedPlan, requestedDuration, paymentProofUrl, userNote).
 */
@IgnoreExtraProperties
data class ProRequest(
    val requestId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    // Plan fields: supports both User App (planId) and Admin (requestedPlan)
    val requestedPlan: String = "pro",
    val planId: String? = null,
    // Duration fields: supports both User App (durationDays) and Admin (requestedDuration)
    val requestedDuration: String = "1 month",
    val durationDays: Long? = null,
    // Payment references: supports both User App (paymentReference) and Admin (paymentProofUrl, paymentMethod)
    val paymentMethod: String? = null,
    val paymentProofUrl: String? = null,
    val paymentReference: String? = null,
    // Notes: supports both User App (notes) and Admin (userNote)
    val userNote: String = "",
    val notes: String? = null,
    // Status & timestamps
    val status: String = ProRequestStatus.PENDING.name,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val cancelledAt: Long? = null,
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val reviewedByEmail: String? = null,
    val rejectionReason: String? = null,
    val adminNote: String? = null
) {
    val statusEnum: ProRequestStatus
        get() = ProRequestStatus.fromString(status)

    val isPending: Boolean
        get() = statusEnum == ProRequestStatus.PENDING

    val isApproved: Boolean
        get() = statusEnum == ProRequestStatus.APPROVED

    val isRejected: Boolean
        get() = statusEnum == ProRequestStatus.REJECTED

    val isCancelled: Boolean
        get() = statusEnum == ProRequestStatus.CANCELLED

    val isReviewed: Boolean
        get() = isApproved || isRejected || isCancelled

    /**
     * Effective plan string reconciling planId and requestedPlan.
     */
    val effectivePlan: String
        get() = when {
            !planId.isNullOrBlank() -> planId
            requestedPlan.isNotBlank() -> requestedPlan
            else -> "pro"
        }

    /**
     * Effective duration in days parsed seamlessly from durationDays or requestedDuration string.
     */
    val effectiveDurationDays: Int
        get() = when {
            durationDays != null && durationDays > 0 -> durationDays.toInt()
            requestedDuration.contains("1 day") || requestedDuration.contains("1d") -> 1
            requestedDuration.contains("7 day") || requestedDuration.contains("7d") -> 7
            requestedDuration.contains("10 day") || requestedDuration.contains("10d") -> 10
            else -> 30
        }

    /**
     * Effective user note reconciling userNote and notes.
     */
    val effectiveNotes: String
        get() = when {
            userNote.isNotBlank() -> userNote
            !notes.isNullOrBlank() -> notes
            else -> ""
        }

    /**
     * Effective payment reference / proof info reconciling paymentReference, paymentProofUrl, and paymentMethod.
     */
    val effectivePaymentReference: String
        get() = when {
            !paymentReference.isNullOrBlank() -> paymentReference
            !paymentProofUrl.isNullOrBlank() -> paymentProofUrl
            !paymentMethod.isNullOrBlank() -> paymentMethod
            else -> ""
        }
}
