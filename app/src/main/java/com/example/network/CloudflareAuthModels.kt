package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Standard error structure returned by Cloudflare Worker API Gateway.
 */
@JsonClass(generateAdapter = true)
data class CloudflareWorkerError(
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null
)

/**
 * Response model for GET /api/v1/auth/verify
 */
@JsonClass(generateAdapter = true)
data class CloudflareAuthVerifyResponse(
    @Json(name = "ok") val ok: Boolean? = null,
    @Json(name = "authenticated") val authenticated: Boolean? = null,
    @Json(name = "projectId") val projectId: String? = null,
    @Json(name = "uid") val uid: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "error") val error: CloudflareWorkerError? = null,
    @Json(name = "message") val message: String? = null
)

/**
 * Response model for GET /api/v1/admin/verify
 */
@JsonClass(generateAdapter = true)
data class CloudflareAdminVerifyResponse(
    @Json(name = "ok") val ok: Boolean? = null,
    @Json(name = "authenticated") val authenticated: Boolean? = null,
    @Json(name = "authorized") val authorized: Boolean? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "projectId") val projectId: String? = null,
    @Json(name = "uid") val uid: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "error") val error: CloudflareWorkerError? = null,
    @Json(name = "message") val message: String? = null
)

/**
 * Error container to safely parse HTTP 4xx / 5xx error payloads without risking deserialization exceptions.
 */
@JsonClass(generateAdapter = true)
data class CloudflareRawErrorResponse(
    @Json(name = "ok") val ok: Boolean? = null,
    @Json(name = "error") val error: CloudflareWorkerError? = null,
    @Json(name = "message") val message: String? = null
)

/**
 * Typed diagnostic outcomes representing all possible authentication, authorization,
 * network, and validation states.
 */
sealed interface CloudflareDiagnosticResult {
    object NoAuthenticatedUser : CloudflareDiagnosticResult
    data class FirebaseTokenRetrievalFailed(val detail: String) : CloudflareDiagnosticResult
    data class AuthVerifiedAdminVerified(val projectId: String, val uid: String, val role: String) : CloudflareDiagnosticResult
    data class AuthVerifiedAdminDenied(val projectId: String, val uid: String, val httpStatus: Int, val code: String?, val message: String?) : CloudflareDiagnosticResult
    data class AuthRequired(val httpStatus: Int, val message: String?) : CloudflareDiagnosticResult
    data class InvalidIdToken(val httpStatus: Int, val message: String?) : CloudflareDiagnosticResult
    data class AdminAccessDenied(val httpStatus: Int, val message: String?) : CloudflareDiagnosticResult
    data class AdminAuthorizationNotConfigured(val httpStatus: Int, val message: String?) : CloudflareDiagnosticResult
    data class AuthVerificationUnavailable(val httpStatus: Int, val message: String?) : CloudflareDiagnosticResult
    data class NetworkFailure(val message: String) : CloudflareDiagnosticResult
    data class WorkerServerError(val httpStatus: Int, val code: String?, val message: String?) : CloudflareDiagnosticResult
    data class InvalidJsonResponse(val httpStatus: Int, val message: String) : CloudflareDiagnosticResult
    data class ProjectIdMismatch(val expected: String, val actual: String) : CloudflareDiagnosticResult
    data class UidMismatch(val expected: String, val actual: String) : CloudflareDiagnosticResult
    data class UnexpectedServerResponse(val httpStatus: Int, val message: String) : CloudflareDiagnosticResult
}

/**
 * Observable UI state encapsulating the full diagnostic execution report.
 */
data class CloudflareDiagnosticReport(
    val hasExecuted: Boolean = false,
    val isRunning: Boolean = false,

    // Stage 1: Local Firebase SDK Token
    val tokenRetrieved: Boolean = false,
    val tokenIssuedAtFormatted: String = "",
    val tokenExpiresAtFormatted: String = "",
    val tokenProviderClaim: String = "",

    // Stage 2: Cloudflare Auth Verification (/api/v1/auth/verify)
    val authExecuted: Boolean = false,
    val authHttpStatus: Int? = null,
    val authOk: Boolean = false,
    val authProjectId: String? = null,
    val authUid: String? = null,
    val authUidMatched: Boolean = false,
    val authErrorMessage: String? = null,

    // Stage 3: Cloudflare Admin Verification (/api/v1/admin/verify)
    val adminExecuted: Boolean = false,
    val adminHttpStatus: Int? = null,
    val adminAuthorized: Boolean = false,
    val adminRole: String? = null,
    val adminErrorMessage: String? = null,

    // Overall Result
    val outcome: CloudflareDiagnosticResult? = null,
    val safeSummaryText: String = ""
)
