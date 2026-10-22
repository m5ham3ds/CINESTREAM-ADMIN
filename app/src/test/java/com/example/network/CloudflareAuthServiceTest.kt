package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class CloudflareAuthServiceTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val service = CloudflareAuthService()

    @Test
    fun testParseAuthRequiredError() {
        val rawJson = """{"ok":false,"error":{"code":"AUTH_REQUIRED","message":"A Firebase ID token is required."}}"""
        val responseBody = rawJson.toResponseBody()
        val error = service.parseErrorBody(responseBody)

        assertNotNull(error)
        assertEquals("AUTH_REQUIRED", error?.code)
        assertEquals("A Firebase ID token is required.", error?.message)
    }

    @Test
    fun testParseInvalidTokenError() {
        val rawJson = """{"ok":false,"error":{"code":"INVALID_ID_TOKEN","message":"The Firebase ID token is invalid or expired."}}"""
        val responseBody = rawJson.toResponseBody()
        val error = service.parseErrorBody(responseBody)

        assertNotNull(error)
        assertEquals("INVALID_ID_TOKEN", error?.code)
        assertEquals("The Firebase ID token is invalid or expired.", error?.message)
    }

    @Test
    fun testParseAdminAccessDeniedError() {
        val rawJson = """{"ok":false,"error":{"code":"ADMIN_ACCESS_DENIED","message":"User is not in the administrator allowlist."}}"""
        val responseBody = rawJson.toResponseBody()
        val error = service.parseErrorBody(responseBody)

        assertNotNull(error)
        assertEquals("ADMIN_ACCESS_DENIED", error?.code)
        assertEquals("User is not in the administrator allowlist.", error?.message)
    }

    @Test
    fun testParseAdminNotConfiguredError() {
        val rawJson = """{"ok":false,"error":{"code":"ADMIN_AUTHORIZATION_NOT_CONFIGURED","message":"Worker ADMIN_UIDS environment variable is missing."}}"""
        val responseBody = rawJson.toResponseBody()
        val error = service.parseErrorBody(responseBody)

        assertNotNull(error)
        assertEquals("ADMIN_AUTHORIZATION_NOT_CONFIGURED", error?.code)
    }

    @Test
    fun testParseMalformedHtmlErrorGracefully() {
        val htmlBody = "<html><body>502 Bad Gateway</body></html>".toResponseBody()
        val error = service.parseErrorBody(htmlBody)

        assertNotNull(error)
        assertEquals("PARSING_ERROR", error?.code)
        assertTrue(error?.message?.contains("Non-JSON") == true)
    }

    @Test
    fun testDeserializeAuthSuccessResponse() {
        val json = """
        {
          "ok": true,
          "authenticated": true,
          "projectId": "cinestream-sulo",
          "uid": "admin_uid_123",
          "email": "sulopros01@gmail.com"
        }
        """.trimIndent()

        val adapter = moshi.adapter(CloudflareAuthVerifyResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals(true, response?.ok)
        assertEquals(true, response?.authenticated)
        assertEquals("cinestream-sulo", response?.projectId)
        assertEquals("admin_uid_123", response?.uid)
        assertEquals("sulopros01@gmail.com", response?.email)
    }

    @Test
    fun testDeserializeAdminSuccessResponse() {
        val json = """
        {
          "ok": true,
          "authenticated": true,
          "authorized": true,
          "role": "admin",
          "projectId": "cinestream-sulo",
          "uid": "admin_uid_123",
          "email": "sulopros01@gmail.com"
        }
        """.trimIndent()

        val adapter = moshi.adapter(CloudflareAdminVerifyResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals(true, response?.ok)
        assertEquals(true, response?.authenticated)
        assertEquals(true, response?.authorized)
        assertEquals("admin", response?.role)
        assertEquals("cinestream-sulo", response?.projectId)
        assertEquals("admin_uid_123", response?.uid)
    }

    @Test
    fun testTypedDiagnosticResultHierarchy() {
        val verified: CloudflareDiagnosticResult = CloudflareDiagnosticResult.AuthVerifiedAdminVerified(
            projectId = "cinestream-sulo",
            uid = "test_uid",
            role = "admin"
        )
        assertTrue(verified is CloudflareDiagnosticResult.AuthVerifiedAdminVerified)

        val denied: CloudflareDiagnosticResult = CloudflareDiagnosticResult.AdminAccessDenied(
            httpStatus = 403,
            message = "Access denied"
        )
        assertTrue(denied is CloudflareDiagnosticResult.AdminAccessDenied)

        val networkFail: CloudflareDiagnosticResult = CloudflareDiagnosticResult.NetworkFailure("timeout")
        assertTrue(networkFail is CloudflareDiagnosticResult.NetworkFailure)

        val tokenFail: CloudflareDiagnosticResult = CloudflareDiagnosticResult.FirebaseTokenRetrievalFailed("error")
        assertTrue(tokenFail is CloudflareDiagnosticResult.FirebaseTokenRetrievalFailed)

        val authDenied: CloudflareDiagnosticResult = CloudflareDiagnosticResult.AuthVerifiedAdminDenied(
            projectId = "cinestream-sulo",
            uid = "uid123",
            httpStatus = 200,
            code = "NOT_ADMIN",
            message = "Role is user"
        )
        assertTrue(authDenied is CloudflareDiagnosticResult.AuthVerifiedAdminDenied)

        val projMismatch: CloudflareDiagnosticResult = CloudflareDiagnosticResult.ProjectIdMismatch("expected", "actual")
        assertTrue(projMismatch is CloudflareDiagnosticResult.ProjectIdMismatch)

        val uidMismatch: CloudflareDiagnosticResult = CloudflareDiagnosticResult.UidMismatch("expected", "actual")
        assertTrue(uidMismatch is CloudflareDiagnosticResult.UidMismatch)
    }

    @Test
    fun testNullUserReturnsNoAuthenticatedUserOutcome() = kotlinx.coroutines.runBlocking {
        val report = service.runFullDiagnostic(null)
        assertTrue(report.hasExecuted)
        assertFalse(report.isRunning)
        assertFalse(report.tokenRetrieved)
        assertFalse(report.authExecuted)
        assertFalse(report.adminExecuted)
        assertTrue(report.outcome is CloudflareDiagnosticResult.NoAuthenticatedUser)
        assertTrue(report.safeSummaryText.contains("No authenticated Firebase user"))
    }

    @Test
    fun testParseNullAndEmptyErrorBodyGracefully() {
        assertNull(service.parseErrorBody(null))
        val emptyError = service.parseErrorBody("".toResponseBody())
        assertNotNull(emptyError)
        assertEquals("PARSING_ERROR", emptyError?.code)
    }

    @Test
    fun testCloudflareWorkerEndpointsMatchSpecification() {
        assertEquals("https://cinestream-api-gateway.blackmyemail1.workers.dev/", CloudflareAuthService.BASE_URL)
        assertEquals("cinestream-sulo", CloudflareAuthService.EXPECTED_PROJECT_ID)
    }
}
