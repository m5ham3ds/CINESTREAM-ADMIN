package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diagnostics.AppLogger
import com.example.models.User
import com.example.repository.AdminRepository
import com.example.state.AppSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LastAuthDiagnostic(
    val authMethod: String = "",
    val timestamp: Long = 0L,
    val exceptionClass: String = "",
    val errorCode: String = "",
    val rawMessage: String = "",
    val causeMessage: String? = null,
    val firebaseAppName: String = "",
    val firebaseProjectId: String = "",
    val isDefaultApp: Boolean = true
)

data class ProbeResult(
    val httpStatusCode: Int = 0,
    val statusCategory: String = "",
    val reasonCode: String = "",
    val serverMessage: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isResetLoading: Boolean = false,
    val resetMessage: String? = null,
    val resetError: String? = null,
    val lastAuthDiagnostic: LastAuthDiagnostic? = null,
    val probeResult: ProbeResult? = null,
    val isProbeLoading: Boolean = false
)

class LoginViewModel(private val repository: AdminRepository = AdminRepository()) : ViewModel() {
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    fun getAuthInstance(): FirebaseAuth = auth
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    init {
        try {
            val currentUser = try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                AppLogger.w("LoginViewModel", "FirebaseAuth currentUser check exception: ${e.message}")
                null
            }

            if (currentUser != null) {
                // If local session exists or user is project owner, immediately permit offline dashboard access
                val isVerifiedBootstrapOwner =
                    currentUser.email?.equals("sulopros01@gmail.com", ignoreCase = true) == true &&
                    currentUser.isEmailVerified
                val hasLocalAdminSession =
                    AppSettings.hasActiveAdminSession(currentUser.uid) || isVerifiedBootstrapOwner

                if (hasLocalAdminSession) {
                    _uiState.value = _uiState.value.copy(isLoggedIn = true)
                }

                viewModelScope.launch {
                    try {
                        val isAdmin = repository.checkIsAdmin(currentUser)
                        if (isAdmin) {
                            AppSettings.setAdminSession(currentUser.uid, currentUser.email ?: "")
                            _uiState.value = _uiState.value.copy(isLoggedIn = true)
                        } else {
                            // Immediate session revocation: server-side authority check rejected admin status
                            AppSettings.clearAdminSession()
                            try { auth.signOut() } catch (_: Exception) {}
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = false,
                                error = "Access Denied: You do not have administrator permissions."
                            )
                        }
                    } catch (e: Exception) {
                        if (!hasLocalAdminSession) {
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = false,
                                error = "Error verifying admin rights: ${e.message}"
                            )
                        }
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = false,
                    error = com.example.AppState.firebaseInitError
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Init: ${com.example.AppState.firebaseInitError ?: e.message}"
            )
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please fill in all fields")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        try {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        checkAndCreateUserProfile(task.result?.user ?: auth.currentUser)
                    } else {
                        val ex = task.exception
                        val errorCode = (ex as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                            ?: (ex as? com.google.firebase.FirebaseException)?.javaClass?.simpleName
                            ?: "UNKNOWN"
                        val currentApp = try { auth.app } catch (_: Exception) { null }
                        val diag = LastAuthDiagnostic(
                            authMethod = "Email/Password",
                            timestamp = System.currentTimeMillis(),
                            exceptionClass = ex?.javaClass?.name ?: "Unknown",
                            errorCode = errorCode,
                            rawMessage = ex?.message ?: "Login failed",
                            causeMessage = ex?.cause?.message,
                            firebaseAppName = currentApp?.name ?: "Unknown",
                            firebaseProjectId = currentApp?.options?.projectId ?: "Unknown",
                            isDefaultApp = currentApp?.name == com.google.firebase.FirebaseApp.DEFAULT_APP_NAME
                        )
                        AppLogger.w("LoginViewModel", "Login failure: method=Email, code=$errorCode, exception=${ex?.javaClass?.simpleName}, msg=${ex?.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = task.exception?.message ?: "Login failed",
                            lastAuthDiagnostic = diag
                        )
                    }
                }
        } catch (e: Exception) {
            val currentApp = try { auth.app } catch (_: Exception) { null }
            val diag = LastAuthDiagnostic(
                authMethod = "Email/Password (Client Exception)",
                timestamp = System.currentTimeMillis(),
                exceptionClass = e.javaClass.name,
                errorCode = "CLIENT_EXCEPTION",
                rawMessage = e.message ?: "Exception",
                causeMessage = e.cause?.message,
                firebaseAppName = currentApp?.name ?: "Unknown",
                firebaseProjectId = currentApp?.options?.projectId ?: "Unknown",
                isDefaultApp = currentApp?.name == com.google.firebase.FirebaseApp.DEFAULT_APP_NAME
            )
            AppLogger.e("LoginViewModel", "Login synchronous exception", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Login Error: ${e.message}",
                lastAuthDiagnostic = diag
            )
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        if (idToken.isBlank()) {
            setError("Google Sign-in returned an empty ID token. Please try again.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isGoogleLoading = true,
            error = null
        )

        try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        checkAndCreateUserProfile(task.result?.user ?: auth.currentUser)
                    } else {
                        val ex = task.exception
                        val errorCode = (ex as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                            ?: (ex as? com.google.firebase.FirebaseException)?.javaClass?.simpleName
                            ?: "UNKNOWN"
                        val currentApp = try { auth.app } catch (_: Exception) { null }
                        val diag = LastAuthDiagnostic(
                            authMethod = "Google Credential",
                            timestamp = System.currentTimeMillis(),
                            exceptionClass = ex?.javaClass?.name ?: "Unknown",
                            errorCode = errorCode,
                            rawMessage = ex?.localizedMessage ?: "Google Sign-in failed",
                            causeMessage = ex?.cause?.message,
                            firebaseAppName = currentApp?.name ?: "Unknown",
                            firebaseProjectId = currentApp?.options?.projectId ?: "Unknown",
                            isDefaultApp = currentApp?.name == com.google.firebase.FirebaseApp.DEFAULT_APP_NAME
                        )
                        AppLogger.w("LoginViewModel", "Google Sign-In failure: code=$errorCode, exception=${ex?.javaClass?.simpleName}, msg=${ex?.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isGoogleLoading = false,
                            error = task.exception?.localizedMessage ?: "Google Sign-in failed. Check network, OAuth client configuration, and Firebase Authentication settings.",
                            lastAuthDiagnostic = diag
                        )
                    }
                }
        } catch (e: Exception) {
            AppLogger.e("LoginViewModel", "Google Firebase credential exchange failed", e)
            val currentApp = try { auth.app } catch (_: Exception) { null }
            val diag = LastAuthDiagnostic(
                authMethod = "Google Credential (Client Exception)",
                timestamp = System.currentTimeMillis(),
                exceptionClass = e.javaClass.name,
                errorCode = "CLIENT_EXCEPTION",
                rawMessage = e.localizedMessage ?: "Exception",
                causeMessage = e.cause?.message,
                firebaseAppName = currentApp?.name ?: "Unknown",
                firebaseProjectId = currentApp?.options?.projectId ?: "Unknown",
                isDefaultApp = currentApp?.name == com.google.firebase.FirebaseApp.DEFAULT_APP_NAME
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isGoogleLoading = false,
                error = e.localizedMessage ?: "Google Sign-in could not be completed. Please try again.",
                lastAuthDiagnostic = diag
            )
        }
    }

    fun runIdentityToolkitProbe(runtimeCertSha1: String) {
        _uiState.value = _uiState.value.copy(isProbeLoading = true)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = auth.app.options.apiKey.trim()
                val packageName = auth.app.applicationContext.packageName
                val certHex = runtimeCertSha1.replace(":", "").lowercase()

                val url = java.net.URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    if (packageName.isNotBlank()) setRequestProperty("X-Android-Package", packageName)
                    if (certHex.isNotBlank() && !certHex.contains("error", ignoreCase = true)) {
                        setRequestProperty("X-Android-Cert", certHex)
                    }
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                }

                val requestJson = """{"email":"connectivity-probe@cinestream.invalid","password":"probepassword","returnSecureToken":true}"""
                conn.outputStream.use { os ->
                    os.write(requestJson.toByteArray(Charsets.UTF_8))
                }

                val statusCode = conn.responseCode
                val responseText = try {
                    val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
                    stream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) {
                    ""
                }

                val hasApiKeyInvalid = responseText.contains("API_KEY_INVALID", ignoreCase = true) ||
                        responseText.contains("API key not valid", ignoreCase = true)
                val hasServiceBlocked = responseText.contains("API_KEY_SERVICE_BLOCKED", ignoreCase = true) ||
                        responseText.contains("not authorized to use this service", ignoreCase = true)
                val hasAndroidAppBlocked = responseText.contains("API_KEY_ANDROID_APP_BLOCKED", ignoreCase = true) ||
                        responseText.contains("Android client application", ignoreCase = true)
                val hasInvalidCredentials = responseText.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                        responseText.contains("EMAIL_NOT_FOUND", ignoreCase = true)

                val probeResult = when {
                    hasInvalidCredentials -> {
                        ProbeResult(
                            httpStatusCode = statusCode,
                            statusCategory = "REST_KEY_VALID",
                            reasonCode = "INVALID_LOGIN_CREDENTIALS",
                            serverMessage = "API key accepted by Identity Toolkit",
                            details = "Identity Toolkit accepted the API key at HTTP REST level! The key exists, is enabled, and is valid."
                        )
                    }
                    hasApiKeyInvalid -> {
                        ProbeResult(
                            httpStatusCode = statusCode,
                            statusCategory = "API_KEY_INVALID",
                            reasonCode = "API_KEY_INVALID",
                            serverMessage = "API key not valid",
                            details = "Google Cloud rejected the API key string itself as invalid. Check that the key exists in Google Cloud Console."
                        )
                    }
                    hasServiceBlocked -> {
                        ProbeResult(
                            httpStatusCode = statusCode,
                            statusCategory = "API_KEY_SERVICE_BLOCKED",
                            reasonCode = "API_KEY_SERVICE_BLOCKED",
                            serverMessage = "Identity Toolkit API blocked",
                            details = "Google Cloud API restrictions on this key do not allow Identity Toolkit API. Add Identity Toolkit API to the allowed APIs in Google Cloud Console."
                        )
                    }
                    hasAndroidAppBlocked -> {
                        ProbeResult(
                            httpStatusCode = statusCode,
                            statusCategory = "API_KEY_ANDROID_APP_BLOCKED",
                            reasonCode = "API_KEY_ANDROID_APP_BLOCKED",
                            serverMessage = "Android package or SHA-1 rejected",
                            details = "Google Cloud Application Restrictions rejected package name or SHA-1 certificate fingerprint. Add package and SHA-1 in Google Cloud Console."
                        )
                    }
                    statusCode == 403 -> {
                        ProbeResult(
                            httpStatusCode = statusCode,
                            statusCategory = "FORBIDDEN_403",
                            reasonCode = "PERMISSION_DENIED",
                            serverMessage = responseText.take(150),
                            details = "Google Cloud returned 403 Forbidden. Check restrictions in Google Cloud Console."
                        )
                    }
                    statusCode in 200..299 -> {
                        ProbeResult(
                            httpStatusCode = statusCode,
                            statusCategory = "REST_KEY_VALID",
                            reasonCode = "SUCCESS",
                            serverMessage = "Success",
                            details = "Identity Toolkit returned HTTP $statusCode. Key is working."
                        )
                    }
                    else -> {
                        ProbeResult(
                            httpStatusCode = statusCode,
                            statusCategory = "HTTP_$statusCode",
                            reasonCode = "OTHER",
                            serverMessage = responseText.take(150),
                            details = "Identity Toolkit returned HTTP $statusCode: ${responseText.take(200)}"
                        )
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isProbeLoading = false,
                        probeResult = probeResult
                    )
                }
            } catch (e: Exception) {
                val errResult = ProbeResult(
                    httpStatusCode = 0,
                    statusCategory = "NETWORK_ERROR",
                    reasonCode = "EXCEPTION",
                    serverMessage = e.localizedMessage ?: "Connection failure",
                    details = "Could not reach identitytoolkit.googleapis.com. Check internet connection: ${e.message}"
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isProbeLoading = false,
                        probeResult = errResult
                    )
                }
            }
        }
    }

    fun setGoogleSignInLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isGoogleLoading = isLoading)
    }

    fun setError(error: String) {
        _uiState.value = _uiState.value.copy(error = error, isLoading = false, isGoogleLoading = false)
    }

    private fun checkAndCreateUserProfile(user: FirebaseUser?) {
        if (user == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isGoogleLoading = false,
                error = "Google/email sign-in returned no Firebase user. Please try again."
            )
            return
        }

        viewModelScope.launch {
            try {
                // Critical Security Check: Ensure user has admin privileges
                val isAdmin = repository.checkIsAdmin(user)
                if (!isAdmin) {
                    auth.signOut()
                    AppSettings.clearAdminSession()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isGoogleLoading = false,
                        isLoggedIn = false,
                        error = "Google/email authentication succeeded, but this account is not authorized for CineStream Admin. Use the owner account or an enabled /admins/{uid} account."
                    )
                    return@launch
                }

                AppSettings.setAdminSession(user.uid, user.email ?: "")

                // Immediately transition user to authenticated admin state
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isGoogleLoading = false,
                    isLoggedIn = true,
                    error = null
                )

                // Background sync of user profile and audit logging
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val userRef = firestore.collection(com.example.contract.FirebaseCollections.USERS).document(user.uid)
                    val doc = userRef.get().await()

                    if (doc.exists()) {
                        userRef.update(
                            mapOf(
                                "lastLoginTimestamp" to System.currentTimeMillis(),
                                "lastActiveAt" to System.currentTimeMillis()
                            )
                        ).await()
                    } else {
                        val newUser = User(
                            id = user.uid,
                            username = user.displayName ?: user.email?.substringBefore("@") ?: "Admin",
                            email = user.email ?: "",
                            role = "admin",
                            lastLoginTimestamp = System.currentTimeMillis(),
                            lastActiveAt = System.currentTimeMillis(),
                            createdAt = System.currentTimeMillis()
                        )
                        userRef.set(newUser, SetOptions.merge()).await()
                    }

                    repository.logAudit(
                        action = "ADMIN_LOGIN",
                        targetType = "AUTH",
                        targetId = user.uid,
                        details = "Admin logged in successfully (${user.email})"
                    )
                } catch (e: Exception) {
                    com.example.diagnostics.AppLogger.w("LoginViewModel", "Background sync notice: ${e.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isGoogleLoading = false,
                    error = "Admin Verification Error: ${e.localizedMessage ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    fun clearResetMessages() {
        _uiState.value = _uiState.value.copy(resetMessage = null, resetError = null)
    }

    fun resetPassword(email: String) {
        val emailTrimmed = email.trim()
        if (emailTrimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(resetError = "Please enter your email address")
            return
        }

        _uiState.value = _uiState.value.copy(isResetLoading = true, resetError = null, resetMessage = null)

        try {
            auth.sendPasswordResetEmail(emailTrimmed)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            isResetLoading = false,
                            resetMessage = "Password reset email sent"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isResetLoading = false,
                            resetError = task.exception?.message ?: "Failed to send reset email"
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isResetLoading = false,
                resetError = "Reset Error: ${e.message}"
            )
        }
    }
    
    fun logout() {
        auth.signOut()
        _uiState.value = LoginUiState(isLoggedIn = false)
    }
}
