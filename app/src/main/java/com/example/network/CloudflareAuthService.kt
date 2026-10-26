package com.example.network

import com.example.diagnostics.AppLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

interface CloudflareAuthApi {
    @Headers("Accept: application/json")
    @GET("api/v1/auth/verify")
    suspend fun verifyAuth(
        @Header("Authorization") authorization: String
    ): Response<CloudflareAuthVerifyResponse>

    @Headers("Accept: application/json")
    @GET("api/v1/admin/verify")
    suspend fun verifyAdmin(
        @Header("Authorization") authorization: String
    ): Response<CloudflareAdminVerifyResponse>
}

class CloudflareAuthService(
    baseUrl: String = BASE_URL
) {
    companion object {
        const val BASE_URL = "https://cinestream-api-gateway.blackmyemail1.workers.dev/"
        const val EXPECTED_PROJECT_ID = "cinestream-sulo"
        private const val TAG = "CloudflareAuthService"

        @Volatile
        private var INSTANCE: CloudflareAuthService? = null

        fun getInstance(): CloudflareAuthService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudflareAuthService().also { INSTANCE = it }
            }
        }
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val errorAdapter by lazy {
        moshi.adapter(CloudflareRawErrorResponse::class.java)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            // Absolute Security Invariant: Never log raw tokens or Authorization header contents
            if (message.contains("Authorization:", ignoreCase = true) ||
                message.contains("Bearer", ignoreCase = true)) {
                AppLogger.d(TAG, "--> Authorization: Bearer [REDACTED_BY_SECURITY_POLICY]")
            } else {
                AppLogger.d(TAG, message)
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC // Logs method, URL, and status code only. Never headers or bodies.
        }

        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: CloudflareAuthApi by lazy {
        retrofit.create(CloudflareAuthApi::class.java)
    }

    fun parseErrorBody(errorBody: ResponseBody?): CloudflareWorkerError? {
        if (errorBody == null) return null
        return try {
            val raw = errorBody.string()
            val parsed = errorAdapter.fromJson(raw)
            parsed?.error ?: CloudflareWorkerError(code = "GATEWAY_ERROR", message = parsed?.message ?: raw.take(120))
        } catch (e: Exception) {
            CloudflareWorkerError(code = "PARSING_ERROR", message = "Non-JSON response from gateway")
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "N/A"
        return try {
            val millis = if (timestamp < 100_000_000_000L) timestamp * 1000L else timestamp
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(millis))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }

    /**
     * Executes the comprehensive 3-stage diagnostic flow:
     * 1. Safely retrieve Firebase ID token via SDK (preferring non-forced cache first)
     * 2. Call GET /api/v1/auth/verify with Bearer token
     * 3. If authenticated, call GET /api/v1/admin/verify with Bearer token
     */
    suspend fun runFullDiagnostic(
        user: FirebaseUser?
    ): CloudflareDiagnosticReport {
        if (user == null) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                outcome = CloudflareDiagnosticResult.NoAuthenticatedUser,
                safeSummaryText = "No authenticated Firebase user found in session."
            )
        }

        // --- STAGE 1: Obtain actual Firebase ID Token safely ---
        val tokenResult = try {
            // First attempt non-forced retrieval to reuse valid cached token
            user.getIdToken(false).await()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Non-forced getIdToken failed, attempting forced refresh: ${e.message}")
            try {
                // Secondary recovery: force refresh once
                user.getIdToken(true).await()
            } catch (e2: Exception) {
                AppLogger.e(TAG, "Fatal token retrieval error", e2)
                return CloudflareDiagnosticReport(
                    hasExecuted = true,
                    tokenRetrieved = false,
                    outcome = CloudflareDiagnosticResult.FirebaseTokenRetrievalFailed(e2.message ?: "Token retrieval failed"),
                    safeSummaryText = "Failed to obtain Firebase ID token: ${e2.message ?: "Unknown error"}"
                )
            }
        }

        val rawToken = tokenResult.token
        if (rawToken.isNullOrBlank()) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = false,
                outcome = CloudflareDiagnosticResult.FirebaseTokenRetrievalFailed("Returned token string was empty"),
                safeSummaryText = "Firebase SDK returned an empty or blank ID token string."
            )
        }

        val issuedAtFormatted = formatTimestamp(tokenResult.issuedAtTimestamp)
        val expiresAtFormatted = formatTimestamp(tokenResult.expirationTimestamp)
        val provider = tokenResult.signInProvider ?: tokenResult.claims["sign_in_provider"]?.toString() ?: "None"

        // Temporary in-memory header, never stored in properties or state
        val bearerHeader = "Bearer $rawToken"

        // --- STAGE 2: Call GET /api/v1/auth/verify ---
        val authResponse: Response<CloudflareAuthVerifyResponse> = try {
            api.verifyAuth(bearerHeader)
        } catch (e: SocketTimeoutException) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                outcome = CloudflareDiagnosticResult.NetworkFailure("Connection timeout contacting Cloudflare Gateway /auth/verify"),
                safeSummaryText = "Network timeout contacting Cloudflare Gateway /auth/verify."
            )
        } catch (e: IOException) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                outcome = CloudflareDiagnosticResult.NetworkFailure(e.message ?: "Network error during auth verification"),
                safeSummaryText = "Network connection failed: ${e.message ?: "Unable to reach gateway"}"
            )
        } catch (e: Exception) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                outcome = CloudflareDiagnosticResult.WorkerServerError(-1, "CLIENT_EXCEPTION", e.message),
                safeSummaryText = "Unexpected client exception during auth verification: ${e.message}"
            )
        }

        if (!authResponse.isSuccessful) {
            val code = authResponse.code()
            val err = parseErrorBody(authResponse.errorBody())
            val outcome = when (code) {
                401 -> {
                    when (err?.code) {
                        "AUTH_REQUIRED" -> CloudflareDiagnosticResult.AuthRequired(code, err.message)
                        "INVALID_ID_TOKEN" -> CloudflareDiagnosticResult.InvalidIdToken(code, err.message)
                        else -> CloudflareDiagnosticResult.InvalidIdToken(code, err?.message ?: "Unauthorized token")
                    }
                }
                503 -> CloudflareDiagnosticResult.AuthVerificationUnavailable(code, err?.message ?: "Auth verification service unavailable")
                in 500..599 -> CloudflareDiagnosticResult.WorkerServerError(code, err?.code, err?.message)
                else -> CloudflareDiagnosticResult.UnexpectedServerResponse(code, err?.message ?: "HTTP $code")
            }
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = code,
                authOk = false,
                authErrorMessage = err?.message ?: "HTTP $code",
                outcome = outcome,
                safeSummaryText = "Auth Verification failed (HTTP $code): ${err?.message ?: "Unauthorized"}"
            )
        }

        val authBody = authResponse.body()
        if (authBody == null || authBody.ok != true || authBody.authenticated != true) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = false,
                outcome = CloudflareDiagnosticResult.InvalidJsonResponse(authResponse.code(), "Auth response ok or authenticated is false"),
                safeSummaryText = "Invalid auth response payload from Cloudflare Worker."
            )
        }

        if (!authBody.projectId.equals(EXPECTED_PROJECT_ID, ignoreCase = true)) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = false,
                authProjectId = authBody.projectId,
                outcome = CloudflareDiagnosticResult.ProjectIdMismatch(EXPECTED_PROJECT_ID, authBody.projectId ?: "null"),
                safeSummaryText = "Project ID mismatch: Expected $EXPECTED_PROJECT_ID, got ${authBody.projectId}"
            )
        }

        if (authBody.uid != user.uid) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = false,
                authUid = authBody.uid,
                outcome = CloudflareDiagnosticResult.UidMismatch(user.uid, authBody.uid ?: "null"),
                safeSummaryText = "UID mismatch: Token UID does not match current session."
            )
        }

        // --- STAGE 3: Call GET /api/v1/admin/verify ---
        val adminResponse: Response<CloudflareAdminVerifyResponse> = try {
            api.verifyAdmin(bearerHeader)
        } catch (e: SocketTimeoutException) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = true,
                authProjectId = authBody.projectId,
                authUid = authBody.uid,
                authUidMatched = true,
                adminExecuted = true,
                outcome = CloudflareDiagnosticResult.NetworkFailure("Connection timeout contacting /admin/verify"),
                safeSummaryText = "Authenticated on Worker, but timed out reaching Admin verification endpoint."
            )
        } catch (e: IOException) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = true,
                authProjectId = authBody.projectId,
                authUid = authBody.uid,
                authUidMatched = true,
                adminExecuted = true,
                outcome = CloudflareDiagnosticResult.NetworkFailure(e.message ?: "Network error during admin verify"),
                safeSummaryText = "Authenticated on Worker, but network failed during Admin verification: ${e.message}"
            )
        } catch (e: Exception) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = true,
                authProjectId = authBody.projectId,
                authUid = authBody.uid,
                authUidMatched = true,
                adminExecuted = true,
                outcome = CloudflareDiagnosticResult.WorkerServerError(-1, "CLIENT_EXCEPTION", e.message),
                safeSummaryText = "Authenticated on Worker, but unexpected client error on admin verify: ${e.message}"
            )
        }

        if (!adminResponse.isSuccessful) {
            val code = adminResponse.code()
            val err = parseErrorBody(adminResponse.errorBody())
            val outcome = when (code) {
                401 -> CloudflareDiagnosticResult.InvalidIdToken(code, err?.message ?: "Token unauthorized")
                403 -> CloudflareDiagnosticResult.AdminAccessDenied(code, err?.message ?: "UID not in ADMIN_UIDS allowlist")
                503 -> {
                    if (err?.code == "ADMIN_AUTHORIZATION_NOT_CONFIGURED") {
                        CloudflareDiagnosticResult.AdminAuthorizationNotConfigured(code, err.message)
                    } else {
                        CloudflareDiagnosticResult.AuthVerificationUnavailable(code, err?.message ?: "Admin verification unavailable")
                    }
                }
                in 500..599 -> CloudflareDiagnosticResult.WorkerServerError(code, err?.code, err?.message)
                else -> CloudflareDiagnosticResult.UnexpectedServerResponse(code, err?.message ?: "HTTP $code")
            }
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = true,
                authProjectId = authBody.projectId,
                authUid = authBody.uid,
                authUidMatched = true,
                adminExecuted = true,
                adminHttpStatus = code,
                adminAuthorized = false,
                adminErrorMessage = err?.message ?: "HTTP $code",
                outcome = outcome,
                safeSummaryText = if (code == 403) {
                    "Authentication Valid • Admin Authorization Denied: User UID is not in ADMIN_UIDS allowlist."
                } else if (code == 503) {
                    "Admin Authorization Not Configured on Worker (ADMIN_UIDS missing or unreadable)."
                } else {
                    "Admin verification returned HTTP $code: ${err?.message ?: "Denied"}"
                }
            )
        }

        val adminBody = adminResponse.body()
        if (adminBody == null || adminBody.ok != true || adminBody.authenticated != true) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = true,
                authProjectId = authBody.projectId,
                authUid = authBody.uid,
                authUidMatched = true,
                adminExecuted = true,
                adminHttpStatus = adminResponse.code(),
                adminAuthorized = false,
                outcome = CloudflareDiagnosticResult.InvalidJsonResponse(adminResponse.code(), "Admin response ok or authenticated is false"),
                safeSummaryText = "Invalid admin response body from Cloudflare Worker."
            )
        }

        if (adminBody.authorized != true || adminBody.role != "admin") {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = true,
                authProjectId = authBody.projectId,
                authUid = authBody.uid,
                authUidMatched = true,
                adminExecuted = true,
                adminHttpStatus = adminResponse.code(),
                adminAuthorized = false,
                adminRole = adminBody.role,
                outcome = CloudflareDiagnosticResult.AuthVerifiedAdminDenied(
                    projectId = adminBody.projectId ?: EXPECTED_PROJECT_ID,
                    uid = adminBody.uid ?: user.uid,
                    httpStatus = adminResponse.code(),
                    code = "NOT_ADMIN_ROLE",
                    message = "Role is ${adminBody.role ?: "none"}, expected admin"
                ),
                safeSummaryText = "Authentication succeeded, but account is not authorized as an administrator (Role: ${adminBody.role ?: "none"})."
            )
        }

        if (!adminBody.projectId.equals(EXPECTED_PROJECT_ID, ignoreCase = true)) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = true,
                authProjectId = authBody.projectId,
                authUid = authBody.uid,
                authUidMatched = true,
                adminExecuted = true,
                adminHttpStatus = adminResponse.code(),
                adminAuthorized = false,
                outcome = CloudflareDiagnosticResult.ProjectIdMismatch(EXPECTED_PROJECT_ID, adminBody.projectId ?: "null"),
                safeSummaryText = "Project ID mismatch in admin verification: ${adminBody.projectId}"
            )
        }

        if (adminBody.uid != user.uid) {
            return CloudflareDiagnosticReport(
                hasExecuted = true,
                tokenRetrieved = true,
                tokenIssuedAtFormatted = issuedAtFormatted,
                tokenExpiresAtFormatted = expiresAtFormatted,
                tokenProviderClaim = provider,
                authExecuted = true,
                authHttpStatus = authResponse.code(),
                authOk = true,
                authProjectId = authBody.projectId,
                authUid = authBody.uid,
                authUidMatched = true,
                adminExecuted = true,
                adminHttpStatus = adminResponse.code(),
                adminAuthorized = false,
                outcome = CloudflareDiagnosticResult.UidMismatch(user.uid, adminBody.uid ?: "null"),
                safeSummaryText = "UID mismatch in admin verification: ${adminBody.uid}"
            )
        }

        // ALL 3 STAGES VERIFIED!
        return CloudflareDiagnosticReport(
            hasExecuted = true,
            tokenRetrieved = true,
            tokenIssuedAtFormatted = issuedAtFormatted,
            tokenExpiresAtFormatted = expiresAtFormatted,
            tokenProviderClaim = provider,
            authExecuted = true,
            authHttpStatus = authResponse.code(),
            authOk = true,
            authProjectId = adminBody.projectId,
            authUid = adminBody.uid,
            authUidMatched = true,
            adminExecuted = true,
            adminHttpStatus = adminResponse.code(),
            adminAuthorized = true,
            adminRole = adminBody.role,
            outcome = CloudflareDiagnosticResult.AuthVerifiedAdminVerified(
                projectId = EXPECTED_PROJECT_ID,
                uid = user.uid,
                role = adminBody.role ?: "admin"
            ),
            safeSummaryText = "All checks passed: Cloudflare Gateway verified valid Firebase ID Token and confirmed Admin authorization (Role: ${adminBody.role})."
        )
    }
}
