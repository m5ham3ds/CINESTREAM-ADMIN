package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.security.MessageDigest
import java.security.SecureRandom
import android.content.pm.PackageManager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.state.AppLanguage
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.CineStreamOutlinedLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.LoginViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import com.example.AppState
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun generateGoogleNonce(): String {
    val randomBytes = ByteArray(32)
    SecureRandom().nextBytes(randomBytes)
    return MessageDigest.getInstance("SHA-256").digest(randomBytes)
        .joinToString("") { "%02x".format(it) }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val currentLang by AppSettings.language.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = context.getString(R.string.default_web_client_id)

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                viewModel.clearResetMessages()
            },
            title = { Text(AppStrings.resetPasswordTitle(currentLang), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        AppStrings.resetPasswordDesc(currentLang),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text(AppStrings.adminEmail(currentLang), color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CineStreamRed,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        )
                    )
                    if (uiState.resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.resetMessage!!, color = MetricGreen, fontSize = 12.sp)
                    }
                    if (uiState.resetError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.resetError!!, color = MetricRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = { viewModel.resetPassword(resetEmail) },
                    isLoading = uiState.isResetLoading,
                    enabled = !uiState.isResetLoading && resetEmail.isNotBlank(),
                    containerColor = CineStreamRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.sendResetLink(currentLang),
                    loadingText = AppStrings.processing(currentLang)
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        viewModel.clearResetMessages()
                    }
                ) {
                    Text(AppStrings.cancel(currentLang), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
    ) {
        AdaptiveScreenContainer(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Language Switcher at Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .background(DarkSurface, RoundedCornerShape(20.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                        .bounceClick(scaleDown = 0.92f, shape = RoundedCornerShape(20.dp)) {
                            val nextLang = if (currentLang == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
                            AppSettings.setLanguage(nextLang)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = CineStreamRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLang == AppLanguage.ARABIC) "English" else "العربية",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CineStream Red Play Icon Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(CineStreamRed, CineStreamDarkRed)),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CineStream Admin Header
            Text(
                text = buildAnnotatedString {
                    append(AppStrings.cine(currentLang))
                    withStyle(style = SpanStyle(color = CineStreamRed)) { append(AppStrings.stream(currentLang)) }
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(AppStrings.adminPortal(currentLang), color = TextSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(24.dp))

            // Main Login Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(AppStrings.loginTitle(currentLang), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(AppStrings.loginSubtitle(currentLang), color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    CustomTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChanged,
                        label = AppStrings.adminEmail(currentLang),
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CustomTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = AppStrings.password(currentLang),
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = CineStreamRed, uncheckedColor = TextSecondary)
                            )
                            Text(AppStrings.rememberMe(currentLang), color = TextSecondary, fontSize = 13.sp)
                        }
                        Text(
                            AppStrings.forgotPassword(currentLang),
                            color = CineStreamRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.bounceClick(scaleDown = 0.95f) {
                                resetEmail = uiState.email
                                showForgotPasswordDialog = true
                            }
                        )
                    }

                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.error!!, color = MetricRed, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sign In Button (CineStream Red) with tactile bounce & loading animation
                    CineStreamLoadingButton(
                        onClick = { viewModel.login() },
                        isLoading = uiState.isLoading,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        containerColor = CineStreamRed,
                        shape = RoundedCornerShape(12.dp),
                        text = AppStrings.signInButton(currentLang),
                        loadingText = AppStrings.processing(currentLang)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkCardBorder)
                        Text(AppStrings.orDivider(currentLang), color = TextSecondary, modifier = Modifier.padding(horizontal = 14.dp), fontSize = 12.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkCardBorder)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Explicit Google sign-in must use GetSignInWithGoogleOption. Unlike GetGoogleIdOption where
                    // setAutoSelectEnabled(false) is needed, GetSignInWithGoogleOption is strictly explicit by design.
                    CineStreamOutlinedLoadingButton(
                        onClick = {
                            val activity = context.findActivity()
                            if (activity == null) {
                                viewModel.setError("Google Sign-In requires an active Android Activity. Please reopen the login screen.")
                            } else if (!webClientId.trim().endsWith(".apps.googleusercontent.com") || webClientId.contains("mock", ignoreCase = true)) {
                                viewModel.setError("Google Sign-In is not configured: the Firebase Web OAuth client ID is missing or invalid.")
                            } else {
                                viewModel.setGoogleSignInLoading(true)
                                coroutineScope.launch {
                                    try {
                                        val credentialManager = CredentialManager.create(activity)
                                        val googleOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId.trim())
                                            .setNonce(generateGoogleNonce())
                                            .build()
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleOption)
                                            .build()
                                        val result = credentialManager.getCredential(activity, request)
                                        val credential = result.credential
                                        val supportedGoogleToken = credential is CustomCredential && (
                                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
                                        )
                                        if (supportedGoogleToken && credential is CustomCredential) {
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                            if (googleIdTokenCredential.idToken.isBlank()) {
                                                viewModel.setError("Google Sign-In returned an empty ID token. Please try again.")
                                            } else {
                                                viewModel.signInWithGoogleToken(googleIdTokenCredential.idToken)
                                            }
                                        } else {
                                            viewModel.setError("Google Sign-In returned an unsupported credential type.")
                                        }
                                    } catch (_: GetCredentialCancellationException) {
                                        // User cancelled the system account picker. This is not an authentication failure.
                                        viewModel.setGoogleSignInLoading(false)
                                    } catch (e: NoCredentialException) {
                                        viewModel.setError("No Google account credential is available. Add a Google account to this device and try again.")
                                    } catch (e: GetCredentialException) {
                                        viewModel.setError("Google account selection failed: ${e.message ?: e.javaClass.simpleName}")
                                    } catch (e: Exception) {
                                        viewModel.setError(e.localizedMessage ?: "Google Sign-In failed. Check network and Firebase Authentication settings.")
                                    }
                                }
                            }
                        },
                        isLoading = uiState.isGoogleLoading,
                        enabled = !uiState.isLoading && !uiState.isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        containerColor = DarkSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        borderColor = DarkCardBorder,
                        text = AppStrings.signInWithGoogle(currentLang),
                        loadingText = AppStrings.processing(currentLang)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = AppStrings.restrictedAccess(currentLang),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(16.dp))
                DebugFirebaseDiagnosticCard(viewModel = viewModel)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Firebase Authentication • CineStream Security", color = TextSecondary, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
    }

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = TextSecondary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle Password Visibility",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceVariant,
            unfocusedContainerColor = DarkSurfaceVariant,
            focusedBorderColor = CineStreamRed,
            unfocusedBorderColor = DarkCardBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        singleLine = true
    )
}

private fun getAppSigningCertificateSha1(context: Context): String {
    return try {
        val pm = context.packageManager
        val packageName = context.packageName
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val packageInfo = pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        val cert = signatures?.firstOrNull()?.toByteArray() ?: return "NO_CERT"
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(cert)
        digest.joinToString(":") { "%02X".format(it) }
    } catch (e: Exception) {
        "ERROR: ${e.message}"
    }
}

data class FirebaseRuntimeDiagnostics(
    val buildApplicationId: String,
    val runtimePackageName: String,
    val isPackageMatched: Boolean,
    val buildType: String,
    val versionName: String,
    val versionCode: Int,
    val runtimeSigningCertSha1: String,
    val expectedSigningCertSha1: String,
    val isSigningCertMatched: Boolean,
    val activeFirebaseAppName: String,
    val isDefaultAppInitialized: Boolean,
    val totalAppsCount: Int,
    val runtimeProjectId: String,
    val runtimeApplicationId: String,
    val vmAuthAppName: String,
    val vmAuthProjectId: String,
    val vmAuthApplicationId: String,
    val isVmAuthUsingDefaultApp: Boolean,
    val isFirebaseInitializerSuccess: Boolean,
    val firebaseInitError: String?,
    val isApiKeyPresent: Boolean,
    val hasAizaPrefix: Boolean,
    val hasPlaceholderFragment: Boolean,
    val isGeneratedResKeyPresent: Boolean,
    val doesKeyMatchGeneratedResource: Boolean,
    val keySha256Short: String,
    val resKeySha256Short: String,
    val generatedAppId: String,
    val isAppIdMatchingGenerated: Boolean,
    val generatedProjectId: String,
    val isProjectIdMatchingExpected: Boolean,
    val isConfigurationDivergent: Boolean,
    val lastAuthDiagnostic: com.example.viewmodels.LastAuthDiagnostic?,
    val probeResult: com.example.viewmodels.ProbeResult?,
    val isProbeLoading: Boolean
)

@Composable
fun rememberFirebaseRuntimeDiagnostics(
    viewModel: LoginViewModel,
    refreshTrigger: Int
): FirebaseRuntimeDiagnostics {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    return remember(refreshTrigger, uiState.lastAuthDiagnostic, uiState.probeResult, uiState.isProbeLoading) {
        val buildAppId = BuildConfig.APPLICATION_ID
        val pkgName = context.packageName
        val isPkgMatch = buildAppId == pkgName
        val bType = BuildConfig.BUILD_TYPE
        val vName = BuildConfig.VERSION_NAME
        val vCode = BuildConfig.VERSION_CODE

        val expectedCert = "54:A8:14:FB:04:DE:BD:98:3F:33:6E:F8:2E:87:82:0E:DB:76:0F:12"
        val runtimeCert = getAppSigningCertificateSha1(context)
        val isCertMatch = runtimeCert.equals(expectedCert, ignoreCase = true)

        val defaultApp = try { FirebaseApp.getInstance() } catch (_: Exception) { null }
        val appsList = try { FirebaseApp.getApps(context) } catch (_: Exception) { emptyList<FirebaseApp>() }
        val defaultAppName = defaultApp?.name ?: "None (Uninitialized)"
        val defaultOptions = defaultApp?.options

        val runtimeProjId = defaultOptions?.projectId ?: "N/A"
        val runtimeAppId = defaultOptions?.applicationId ?: "N/A"
        val runtimeApiKey = defaultOptions?.apiKey ?: ""

        val vmAuth = try { viewModel.getAuthInstance() } catch (_: Exception) { null }
        val vmAuthApp = try { vmAuth?.app } catch (_: Exception) { null }
        val vmAuthAppName = vmAuthApp?.name ?: "N/A"
        val vmAuthProjId = vmAuthApp?.options?.projectId ?: "N/A"
        val vmAuthAppId = vmAuthApp?.options?.applicationId ?: "N/A"
        val isVmAuthDefault = vmAuthApp != null && defaultApp != null && vmAuthApp == defaultApp

        val isInitSuccess = AppState.firebaseInitialized
        val initError = AppState.firebaseInitError

        val resKeyId = context.resources.getIdentifier("google_api_key", "string", context.packageName)
        val resApiKey = if (resKeyId != 0) try { context.getString(resKeyId) } catch (_: Exception) { null } else null

        val resAppIdRes = context.resources.getIdentifier("google_app_id", "string", context.packageName)
        val resAppId = if (resAppIdRes != 0) try { context.getString(resAppIdRes) } catch (_: Exception) { null } else null

        val resProjIdRes = context.resources.getIdentifier("project_id", "string", context.packageName)
        val resProjId = if (resProjIdRes != 0) try { context.getString(resProjIdRes) } catch (_: Exception) { null } else null

        fun computeSha256Prefix(value: String?): String {
            if (value.isNullOrBlank()) return "N/A"
            return try {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(value.trim().toByteArray(Charsets.UTF_8))
                digest.joinToString("") { "%02x".format(it) }.take(8)
            } catch (_: Exception) {
                "ERR"
            }
        }

        val isApiKeyPresent = runtimeApiKey.isNotBlank()
        val hasAizaPrefix = runtimeApiKey.startsWith("AIza")
        val hasPlaceholder = runtimeApiKey.contains("remix", ignoreCase = true) ||
                runtimeApiKey.contains("placeholder", ignoreCase = true) ||
                runtimeApiKey.contains("YOUR_API_KEY", ignoreCase = true)

        val isResKeyPresent = !resApiKey.isNullOrBlank()
        val doesKeyMatch = isResKeyPresent && runtimeApiKey == resApiKey
        val keySha = computeSha256Prefix(runtimeApiKey)
        val resKeySha = computeSha256Prefix(resApiKey)

        val isAppIdMatch = resAppId != null && runtimeAppId == resAppId
        val isProjIdMatch = runtimeProjId.equals("cinestream-sulo", ignoreCase = true)

        val isDivergent = (resApiKey != null && runtimeApiKey != resApiKey) ||
                (resAppId != null && runtimeAppId != resAppId) ||
                (resProjId != null && runtimeProjId != resProjId)

        FirebaseRuntimeDiagnostics(
            buildApplicationId = buildAppId,
            runtimePackageName = pkgName,
            isPackageMatched = isPkgMatch,
            buildType = bType,
            versionName = vName,
            versionCode = vCode,
            runtimeSigningCertSha1 = runtimeCert,
            expectedSigningCertSha1 = expectedCert,
            isSigningCertMatched = isCertMatch,
            activeFirebaseAppName = defaultAppName,
            isDefaultAppInitialized = defaultApp != null,
            totalAppsCount = appsList.size,
            runtimeProjectId = runtimeProjId,
            runtimeApplicationId = runtimeAppId,
            vmAuthAppName = vmAuthAppName,
            vmAuthProjectId = vmAuthProjId,
            vmAuthApplicationId = vmAuthAppId,
            isVmAuthUsingDefaultApp = isVmAuthDefault,
            isFirebaseInitializerSuccess = isInitSuccess,
            firebaseInitError = initError,
            isApiKeyPresent = isApiKeyPresent,
            hasAizaPrefix = hasAizaPrefix,
            hasPlaceholderFragment = hasPlaceholder,
            isGeneratedResKeyPresent = isResKeyPresent,
            doesKeyMatchGeneratedResource = doesKeyMatch,
            keySha256Short = keySha,
            resKeySha256Short = resKeySha,
            generatedAppId = resAppId ?: "Missing",
            isAppIdMatchingGenerated = isAppIdMatch,
            generatedProjectId = resProjId ?: "Missing",
            isProjectIdMatchingExpected = isProjIdMatch,
            isConfigurationDivergent = isDivergent,
            lastAuthDiagnostic = uiState.lastAuthDiagnostic,
            probeResult = uiState.probeResult,
            isProbeLoading = uiState.isProbeLoading
        )
    }
}

@Composable
fun DebugFirebaseDiagnosticCard(viewModel: LoginViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    var refreshCounter by remember { mutableIntStateOf(0) }
    val diag = rememberFirebaseRuntimeDiagnostics(viewModel, refreshCounter)
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (diag.isConfigurationDivergent || !diag.isPackageMatched || !diag.isSigningCertMatched || diag.hasPlaceholderFragment)
                    listOf(MetricRed, MetricRed)
                else
                    listOf(DarkCardBorder, DarkCardBorder)
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = "Diagnostics",
                        tint = if (diag.isPackageMatched && diag.isSigningCertMatched && diag.hasAizaPrefix && !diag.hasPlaceholderFragment) MetricGreen else MetricRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Firebase Diagnostic (DEBUG)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { refreshCounter++ },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DiagnosticBadge(
                    label = if (diag.isPackageMatched) "PKG OK" else "PKG MISMATCH",
                    isSuccess = diag.isPackageMatched,
                    modifier = Modifier.weight(1f)
                )
                DiagnosticBadge(
                    label = if (diag.isSigningCertMatched) "SIGN OK" else "SIGN MISMATCH",
                    isSuccess = diag.isSigningCertMatched,
                    modifier = Modifier.weight(1f)
                )
                DiagnosticBadge(
                    label = if (diag.isDefaultAppInitialized) "APP: OK" else "APP: NONE",
                    isSuccess = diag.isDefaultAppInitialized,
                    modifier = Modifier.weight(1f)
                )
                DiagnosticBadge(
                    label = if (diag.hasAizaPrefix && !diag.hasPlaceholderFragment) "KEY: OK" else "KEY: INVALID",
                    isSuccess = diag.hasAizaPrefix && !diag.hasPlaceholderFragment,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticSectionHeader("A. App & Package Identity")
                DiagnosticItem("Application ID (Build)", diag.buildApplicationId)
                DiagnosticItem("Package Name (Context)", diag.runtimePackageName)
                DiagnosticItem("Package Match", if (diag.isPackageMatched) "MATCHED" else "MISMATCH!", isGood = diag.isPackageMatched)
                DiagnosticItem("Build Type / Variant", "${diag.buildType} (v${diag.versionName} #${diag.versionCode})")

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticSectionHeader("B. Signing Keystore & Fingerprint (Canonical Lock)")
                DiagnosticItem("Runtime APK SHA-1", diag.runtimeSigningCertSha1, isGood = diag.isSigningCertMatched)
                DiagnosticItem("Expected Locked SHA-1", diag.expectedSigningCertSha1)
                DiagnosticItem("Keystore Match", if (diag.isSigningCertMatched) "MATCHED (Canonical Debug)" else "MISMATCH! (Re-signed or local key)", isGood = diag.isSigningCertMatched)

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticSectionHeader("C. Runtime Firebase Options & Instance Sync")
                DiagnosticItem("Default App Initialized", "${diag.isDefaultAppInitialized} (${diag.activeFirebaseAppName})", isGood = diag.isDefaultAppInitialized)
                DiagnosticItem("Total Firebase Apps", "${diag.totalAppsCount}")
                DiagnosticItem("Runtime Project ID", diag.runtimeProjectId, isGood = diag.isProjectIdMatchingExpected)
                DiagnosticItem("Runtime Application ID", diag.runtimeApplicationId)
                DiagnosticItem("ViewModel Auth App", diag.vmAuthAppName)
                DiagnosticItem("VM Uses Default App", if (diag.isVmAuthUsingDefaultApp) "YES (Synced)" else "NO (Different Instance!)", isGood = diag.isVmAuthUsingDefaultApp)
                DiagnosticItem("FirebaseInitializer", if (diag.isFirebaseInitializerSuccess) "SUCCESS" else "FAILED: ${diag.firebaseInitError}", isGood = diag.isFirebaseInitializerSuccess)

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticSectionHeader("D. API Key Integrity (Zero Leaks)")
                DiagnosticItem("API Key Non-Empty", if (diag.isApiKeyPresent) "YES" else "NO (Empty)", isGood = diag.isApiKeyPresent)
                DiagnosticItem("Prefix 'AIza'", if (diag.hasAizaPrefix) "YES" else "NO", isGood = diag.hasAizaPrefix)
                DiagnosticItem("Placeholder Check", if (diag.hasPlaceholderFragment) "FOUND PLACEHOLDER!" else "NONE (Clean)", isGood = !diag.hasPlaceholderFragment)
                DiagnosticItem("Generated Res Key Present", if (diag.isGeneratedResKeyPresent) "YES" else "NO", isGood = diag.isGeneratedResKeyPresent)
                DiagnosticItem("Matches Generated Res", if (diag.doesKeyMatchGeneratedResource) "EXACT MATCH" else "DIVERGENT!", isGood = diag.doesKeyMatchGeneratedResource)
                DiagnosticItem("Key SHA-256 (Prefix)", "${diag.keySha256Short} vs Res: ${diag.resKeySha256Short}")
                DiagnosticItem("Generated App ID", diag.generatedAppId)
                DiagnosticItem("App ID Matches Res", if (diag.isAppIdMatchingGenerated) "MATCHED" else "MISMATCH", isGood = diag.isAppIdMatchingGenerated)
                DiagnosticItem("Config Divergence", if (diag.isConfigurationDivergent) "WARNING: DIVERGENT" else "NONE (Consistent)", isGood = !diag.isConfigurationDivergent)

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticSectionHeader("E. Last Auth Attempt Evidence")
                if (diag.lastAuthDiagnostic != null) {
                    val last = diag.lastAuthDiagnostic
                    DiagnosticItem("Auth Method", last.authMethod)
                    DiagnosticItem("Exception Class", last.exceptionClass, isGood = false)
                    DiagnosticItem("Error Code", last.errorCode, isGood = false)
                    DiagnosticItem("Raw SDK Message", last.rawMessage)
                    if (!last.causeMessage.isNullOrBlank()) {
                        DiagnosticItem("Cause Message", last.causeMessage)
                    }
                    DiagnosticItem("FirebaseApp Used", "${last.firebaseAppName} (${if (last.isDefaultApp) "Default" else "Secondary!"})", isGood = last.isDefaultApp)
                    DiagnosticItem("Project ID Used", last.firebaseProjectId)
                } else {
                    DiagnosticItem("Last Attempt", "No failed auth attempt recorded in current session", isGood = true)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticSectionHeader("F. Live Google Identity Toolkit Probe")
                Text(
                    "Probes identitytoolkit.googleapis.com with active API key and device package/SHA-1 headers to distinguish key validity vs API restrictions vs app restrictions.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (diag.probeResult != null) {
                    val p = diag.probeResult
                    val isProbeGood = p.statusCategory.startsWith("REST_KEY_VALID")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isProbeGood) MetricGreen.copy(alpha = 0.12f) else MetricRed.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isProbeGood) MetricGreen.copy(alpha = 0.4f) else MetricRed.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isProbeGood) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isProbeGood) MetricGreen else MetricRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Status: ${p.statusCategory} [HTTP ${p.httpStatusCode}]",
                                    color = if (isProbeGood) MetricGreen else MetricRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(p.details, color = Color.White, fontSize = 11.sp)
                            if (p.serverMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Response: ${p.serverMessage}", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                CineStreamLoadingButton(
                    onClick = {
                        viewModel.runIdentityToolkitProbe(diag.runtimeSigningCertSha1)
                    },
                    isLoading = diag.isProbeLoading,
                    enabled = !diag.isProbeLoading && diag.isApiKeyPresent,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    containerColor = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    text = "Run Identity Toolkit Probe",
                    loadingText = "Probing Identity Toolkit..."
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val summary = formatDiagnosticsSummary(diag)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Firebase Diagnostics", summary)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Diagnostic Summary", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticBadge(label: String, isSuccess: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                if (isSuccess) MetricGreen.copy(alpha = 0.15f) else MetricRed.copy(alpha = 0.15f),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (isSuccess) MetricGreen.copy(alpha = 0.5f) else MetricRed.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp)
            )
            .padding(vertical = 4.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isSuccess) MetricGreen else MetricRed,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun DiagnosticSectionHeader(title: String) {
    Text(
        title,
        color = CineStreamRed,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun DiagnosticItem(label: String, value: String, isGood: Boolean? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            value,
            color = when (isGood) {
                true -> MetricGreen
                false -> MetricRed
                null -> Color.White
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier.weight(1.5f, fill = false)
        )
    }
}

private fun formatDiagnosticsSummary(d: FirebaseRuntimeDiagnostics): String {
    return buildString {
        appendLine("=== CINESTREAM ADMIN FIREBASE DIAGNOSTICS ===")
        appendLine("App ID (Build): ${d.buildApplicationId}")
        appendLine("Package (Runtime): ${d.runtimePackageName} (Matched: ${d.isPackageMatched})")
        appendLine("Build Variant: ${d.buildType} | v${d.versionName} (${d.versionCode})")
        appendLine("Runtime Signing SHA-1: ${d.runtimeSigningCertSha1}")
        appendLine("Expected Signing SHA-1: ${d.expectedSigningCertSha1} (Matched: ${d.isSigningCertMatched})")
        appendLine("Default FirebaseApp: ${d.isDefaultAppInitialized} [${d.activeFirebaseAppName}]")
        appendLine("Apps Count: ${d.totalAppsCount}")
        appendLine("Runtime Project ID: ${d.runtimeProjectId} (Expected Match: ${d.isProjectIdMatchingExpected})")
        appendLine("Runtime App ID: ${d.runtimeApplicationId}")
        appendLine("VM Auth App: ${d.vmAuthAppName} (Default: ${d.isVmAuthUsingDefaultApp})")
        appendLine("FirebaseInitializer: ${d.isFirebaseInitializerSuccess}")
        if (d.firebaseInitError != null) appendLine("Init Error: ${d.firebaseInitError}")
        appendLine("Key Present: ${d.isApiKeyPresent}")
        appendLine("AIza Prefix: ${d.hasAizaPrefix}")
        appendLine("Placeholder Fragment: ${d.hasPlaceholderFragment}")
        appendLine("Generated Res Key Present: ${d.isGeneratedResKeyPresent}")
        appendLine("Matches Res Key: ${d.doesKeyMatchGeneratedResource}")
        appendLine("Key SHA-256 (8h): Runtime=${d.keySha256Short} vs Res=${d.resKeySha256Short}")
        appendLine("Generated App ID: ${d.generatedAppId} (Matched: ${d.isAppIdMatchingGenerated})")
        appendLine("Configuration Divergence: ${d.isConfigurationDivergent}")
        if (d.lastAuthDiagnostic != null) {
            val l = d.lastAuthDiagnostic
            appendLine("--- LAST AUTH ATTEMPT ---")
            appendLine("Method: ${l.authMethod}")
            appendLine("Exception Class: ${l.exceptionClass}")
            appendLine("Error Code: ${l.errorCode}")
            appendLine("Raw Message: ${l.rawMessage}")
            if (!l.causeMessage.isNullOrBlank()) appendLine("Cause: ${l.causeMessage}")
            appendLine("FirebaseApp: ${l.firebaseAppName} (isDefault: ${l.isDefaultApp})")
            appendLine("Project ID: ${l.firebaseProjectId}")
        }
        if (d.probeResult != null) {
            val p = d.probeResult
            appendLine("--- IDENTITY TOOLKIT PROBE ---")
            appendLine("HTTP Status: ${p.httpStatusCode}")
            appendLine("Category: ${p.statusCategory}")
            appendLine("Reason: ${p.reasonCode}")
            appendLine("Server Message: ${p.serverMessage}")
            appendLine("Details: ${p.details}")
        }
        appendLine("=============================================")
    }
}
