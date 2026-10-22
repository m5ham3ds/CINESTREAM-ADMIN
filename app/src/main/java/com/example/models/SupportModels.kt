package com.example.models

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Lifecycle states for user support conversations.
 * Supported: OPEN, PENDING, RESOLVED, CLOSED.
 *
 * Invariant: Unknown/malformed status safely resolves to CLOSED (never silently OPEN).
 */
enum class SupportConversationStatus(val canonicalValue: String) {
    OPEN("open"),
    PENDING("pending"),
    RESOLVED("resolved"),
    CLOSED("closed");

    companion object {
        fun fromString(value: String?): SupportConversationStatus {
            return when (value?.trim()?.lowercase()) {
                "open" -> OPEN
                "pending" -> PENDING
                "resolved" -> RESOLVED
                "closed" -> CLOSED
                else -> CLOSED // Safe fallback: never silently convert unknown to OPEN/active
            }
        }
    }
}

/**
 * Support Conversation Thread Model (Phase C3 / Phase C5 Remediation).
 * Document Path: /support_conversations/{conversationId}
 */
@IgnoreExtraProperties
data class SupportConversation(
    val conversationId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val subject: String = "",
    val status: String = SupportConversationStatus.OPEN.canonicalValue,
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastMessageTime: Long = 0L, // Compatibility with User App contract (DISC-03)
    val lastSenderRole: String = "user",
    val unreadByAdmin: Boolean = false,
    val unreadByUser: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val statusEnum: SupportConversationStatus
        get() = SupportConversationStatus.fromString(status)

    val isResolvedOrClosed: Boolean
        get() = statusEnum == SupportConversationStatus.RESOLVED || statusEnum == SupportConversationStatus.CLOSED

    val effectiveLastMessageTimestamp: Long
        get() = when {
            lastMessageAt > 0L -> lastMessageAt
            lastMessageTime > 0L -> lastMessageTime
            updatedAt > 0L -> updatedAt
            else -> createdAt
        }
}

/**
 * Support Message Item Model (Phase C3).
 * Document Path: /support_conversations/{conversationId}/messages/{messageId}
 */
@IgnoreExtraProperties
data class SupportMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderRole: String = "user", // "user" or "admin"
    val senderEmail: String = "",
    val text: String = "",
    val mediaUrl: String? = null,
    val timestamp: Long = 0L,
    val read: Boolean = false
) {
    val isAdminMessage: Boolean
        get() = senderRole.equals("admin", ignoreCase = true)
}
