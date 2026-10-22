package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.contract.FirebaseCollections
import com.example.diagnostics.AppLogger
import com.example.repository.AdminRepository
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.CineStreamOutlinedLoadingButton
import com.example.ui.components.bounceClick
import com.example.network.CloudflareAuthService
import com.example.network.CloudflareDiagnosticReport
import com.example.network.CloudflareDiagnosticResult
import com.example.ui.theme.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

enum class AdminAuthorityType {
    CHECKING,
    VERIFIED_OWNER,
    VERIFIED_ADMIN_DOC,
    NOT_VERIFIED
}

data class TokenInspectionState(
    val hasInspected: Boolean = false,
    val isSuccess: Boolean = false,
    val issuedAtFormatted: String = "",
    val expiresAtFormatted: String = "",
    val signInProviderClaim: String = "",
    val errorMessage: String? = null
)

@Composable
fun AdminProfileScreen(
    onLogoutClick: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val currentLang by AppSettings.language.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var currentUser by remember {
        mutableStateOf(
            try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                null
            }
        )
    }

    var authorityType by remember { mutableStateOf(AdminAuthorityType.CHECKING) }
    var isRefreshingProfile by remember { mutableStateOf(false) }
    var isInspectingToken by remember { mutableStateOf(false) }
    var tokenInspection by remember { mutableStateOf(TokenInspectionState()) }
    var diagnosticReport by remember { mutableStateOf(CloudflareDiagnosticReport()) }
    val cloudflareAuthService = remember { CloudflareAuthService.getInstance() }

    val adminRepository = remember { AdminRepository() }

    val activeFirebaseApp = remember {
        try {
            FirebaseApp.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    val activeProjectId = activeFirebaseApp?.options?.projectId ?: "cinestream-sulo"
    val activeAppId = activeFirebaseApp?.options?.applicationId ?: "1:979447256418:android:ea5b570266ae1aa6a9883b"
    val expectedProjectId = "cinestream-sulo"
    val expectedAppId = "1:979447256418:android:ea5b570266ae1aa6a9883b"

    fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return AppStrings.notProvided(currentLang)
        return try {
            val millis = if (timestamp < 100_000_000_000L) timestamp * 1000L else timestamp
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(millis))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }

    // Function to check authoritative admin status
    suspend fun evaluateAuthority(user: FirebaseUser?) {
        if (user == null) {
            authorityType = AdminAuthorityType.NOT_VERIFIED
            return
        }

        val isOwner = user.email?.equals(AdminRepository.OWNER_EMAIL, ignoreCase = true) == true && user.isEmailVerified
        if (isOwner) {
            authorityType = AdminAuthorityType.VERIFIED_OWNER
            return
        }

        try {
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection(FirebaseCollections.ADMINS).document(user.uid).get().await()
            if (doc.exists() && doc.getBoolean("enabled") == true) {
                authorityType = AdminAuthorityType.VERIFIED_ADMIN_DOC
            } else {
                authorityType = AdminAuthorityType.NOT_VERIFIED
            }
        } catch (e: Exception) {
            AppLogger.w("AdminProfileScreen", "Authority check notice: ${e.message}")
            authorityType = AdminAuthorityType.NOT_VERIFIED
        }
    }

    // Initial check
    LaunchedEffect(currentUser?.uid) {
        evaluateAuthority(currentUser)
    }

    AdaptiveScreenContainer(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ========================================================
            // SECTION 1: Profile Header Card
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Circle with Online Dot and optional Profile Photo
                    Box(modifier = Modifier.size(76.dp)) {
                        val photoUrl = currentUser?.photoUrl
                        if (photoUrl != null && photoUrl.toString().isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = AppStrings.profile(currentLang),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, CineStreamRed, CircleShape)
                                    .align(Alignment.Center)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color(0xFF202532), CircleShape)
                                    .border(2.dp, CineStreamRed, CircleShape)
                                    .align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser?.displayName?.firstOrNull()?.toString()
                                        ?: AppStrings.adminLetter(currentLang),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Green Online Status Indicator
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(MetricGreen, CircleShape)
                                .border(3.dp, DarkSurface, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Display Name or Email
                    Text(
                        text = currentUser?.displayName?.ifBlank { null }
                            ?: currentUser?.email
                            ?: AppStrings.welcomeAdmin(currentLang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Email subtext if display name was used above
                    if (!currentUser?.displayName.isNullOrBlank() && !currentUser?.email.isNullOrBlank()) {
                        Text(
                            text = currentUser?.email ?: "",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Status Badges Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Administrator Authority Status Badge
                        when (authorityType) {
                            AdminAuthorityType.CHECKING -> {
                                Box(
                                    modifier = Modifier
                                        .background(TextSecondary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = AppStrings.loading(currentLang),
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            AdminAuthorityType.VERIFIED_OWNER -> {
                                Box(
                                    modifier = Modifier
                                        .background(CineStreamRed.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                        .border(1.dp, CineStreamRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = AppStrings.adminVerifiedOwner(currentLang),
                                        color = CineStreamRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            AdminAuthorityType.VERIFIED_ADMIN_DOC -> {
                                Box(
                                    modifier = Modifier
                                        .background(MetricGreen.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                        .border(1.dp, MetricGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = AppStrings.adminVerifiedRecord(currentLang),
                                        color = MetricGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            AdminAuthorityType.NOT_VERIFIED -> {
                                Box(
                                    modifier = Modifier
                                        .background(WarningOrange.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                        .border(1.dp, WarningOrange.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = AppStrings.adminNotVerified(currentLang),
                                        color = WarningOrange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Email Verification Badge
                        if (currentUser != null) {
                            val isVerified = currentUser?.isEmailVerified == true
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isVerified) MetricGreen.copy(alpha = 0.15f) else WarningOrange.copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isVerified) AppStrings.verified(currentLang) else AppStrings.unverified(currentLang),
                                    color = if (isVerified) MetricGreen else WarningOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Warning notice if administrator authority check failed
                    if (authorityType == AdminAuthorityType.NOT_VERIFIED) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.10f)),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(WarningOrange.copy(alpha = 0.4f), WarningOrange.copy(alpha = 0.4f)))
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = WarningOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = AppStrings.adminNotVerifiedExplanation(currentLang),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // SECTION 2: Profile Details Card
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = CineStreamRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            AppStrings.profileTitle(currentLang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    HorizontalDivider(color = DarkCardBorder)

                    // Display Name
                    ProfileDataRow(
                        label = AppStrings.displayName(currentLang),
                        value = currentUser?.displayName?.ifBlank { null } ?: AppStrings.notProvided(currentLang)
                    )

                    // Email Address
                    ProfileDataRow(
                        label = AppStrings.emailField(currentLang),
                        value = currentUser?.email ?: AppStrings.notProvided(currentLang)
                    )

                    // Firebase UID (with Copy button)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(AppStrings.uidField(currentLang), color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = currentUser?.uid ?: AppStrings.notProvided(currentLang),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (currentUser?.uid != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .bounceClick(scaleDown = 0.85f, shape = CircleShape) {
                                        currentUser?.uid?.let {
                                            clipboardManager.setText(AnnotatedString(it))
                                            Toast.makeText(context, AppStrings.copied(currentLang), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = AppStrings.copy(currentLang),
                                    tint = CineStreamRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Authentication State
                    ProfileDataRow(
                        label = AppStrings.authStatus(currentLang),
                        value = when {
                            currentUser == null -> AppStrings.unauthenticated(currentLang)
                            currentUser?.isAnonymous == true -> AppStrings.anonymousAccount(currentLang)
                            else -> AppStrings.authenticated(currentLang)
                        },
                        valueColor = if (currentUser != null) MetricGreen else MetricRed
                    )

                    // Sign-in Provider(s)
                    val providers = currentUser?.providerData?.mapNotNull { it.providerId }?.filter { it.isNotBlank() } ?: emptyList()
                    val providersFormatted = if (providers.isNotEmpty()) {
                        providers.joinToString(", ") { pid ->
                            when (pid) {
                                "google.com" -> "Google"
                                "password" -> "Email / Password"
                                "anonymous" -> "Anonymous"
                                else -> pid
                            }
                        }
                    } else {
                        AppStrings.notProvided(currentLang)
                    }
                    ProfileDataRow(
                        label = AppStrings.signInProvider(currentLang),
                        value = providersFormatted
                    )

                    // Email Verification
                    ProfileDataRow(
                        label = AppStrings.emailVerificationStatus(currentLang),
                        value = if (currentUser?.isEmailVerified == true) AppStrings.verified(currentLang) else AppStrings.unverified(currentLang),
                        valueColor = if (currentUser?.isEmailVerified == true) MetricGreen else WarningOrange
                    )

                    // Creation Date
                    ProfileDataRow(
                        label = AppStrings.creationDate(currentLang),
                        value = currentUser?.metadata?.creationTimestamp?.let { formatTimestamp(it) } ?: AppStrings.notProvided(currentLang)
                    )

                    // Last Sign-In Time
                    ProfileDataRow(
                        label = AppStrings.lastSignInDate(currentLang),
                        value = currentUser?.metadata?.lastSignInTimestamp?.let { formatTimestamp(it) } ?: AppStrings.notProvided(currentLang)
                    )

                    // Active Firebase Project ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(AppStrings.activeProjectId(currentLang), color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = activeProjectId,
                            color = MetricBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ========================================================
            // SECTION 3: Session & ID Token Diagnostics Card
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = MetricBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            AppStrings.sessionDiagnosticsTitle(currentLang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    HorizontalDivider(color = DarkCardBorder)

                    // FirebaseAuth Instance
                    ProfileDataRow(
                        label = AppStrings.firebaseInstanceStatus(currentLang),
                        value = if (activeFirebaseApp != null) AppStrings.firebaseAvailable(currentLang) else AppStrings.firebaseUnavailable(currentLang),
                        valueColor = if (activeFirebaseApp != null) MetricGreen else MetricRed
                    )

                    // Current Firebase User
                    ProfileDataRow(
                        label = AppStrings.sessionUserStatus(currentLang),
                        value = if (currentUser != null) AppStrings.present(currentLang) else AppStrings.absent(currentLang),
                        valueColor = if (currentUser != null) MetricGreen else MetricRed
                    )

                    // Session Type
                    ProfileDataRow(
                        label = AppStrings.authStatus(currentLang),
                        value = if (currentUser?.isAnonymous == true) AppStrings.anonymousAccount(currentLang) else AppStrings.registeredAccount(currentLang),
                        valueColor = if (currentUser?.isAnonymous == true) WarningOrange else MetricBlue
                    )

                    // UID Consistency with Local Session Cache
                    val cachedUid = AppSettings.cachedAdminUid()
                    val isUidConsistent = currentUser != null && currentUser?.uid == cachedUid
                    ProfileDataRow(
                        label = AppStrings.uidConsistencyLabel(currentLang),
                        value = if (isUidConsistent) AppStrings.uidConsistent(currentLang) else AppStrings.uidInconsistent(currentLang),
                        valueColor = if (isUidConsistent) MetricGreen else WarningOrange
                    )

                    // Target Project ID Match
                    val isProjectMatched = activeProjectId.equals(expectedProjectId, ignoreCase = true)
                    ProfileDataRow(
                        label = AppStrings.projectIdMatch(currentLang),
                        value = if (isProjectMatched) "$activeProjectId (OK)" else "$activeProjectId (Mismatch)",
                        valueColor = if (isProjectMatched) MetricGreen else MetricRed
                    )

                    // Target App ID Match
                    val isAppIdMatched = activeAppId == expectedAppId
                    ProfileDataRow(
                        label = AppStrings.targetAppId(currentLang),
                        value = if (isAppIdMatched) "Verified Admin App ID" else "Unexpected App ID",
                        valueColor = if (isAppIdMatched) MetricGreen else WarningOrange
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Cloudflare Gateway & ID Token Diagnostic Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = AppStrings.cloudflareGatewaySection(currentLang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (diagnosticReport.hasExecuted) {
                                // STAGE 1: Local Firebase ID Token
                                if (diagnosticReport.tokenRetrieved) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MetricGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(AppStrings.idTokenRetrievedSuccess(currentLang), color = MetricGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (diagnosticReport.tokenIssuedAtFormatted.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(AppStrings.idTokenIssuedAt(currentLang), color = TextSecondary, fontSize = 11.sp)
                                            Text(diagnosticReport.tokenIssuedAtFormatted, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    if (diagnosticReport.tokenExpiresAtFormatted.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(AppStrings.idTokenExpiresAt(currentLang), color = TextSecondary, fontSize = 11.sp)
                                            Text(diagnosticReport.tokenExpiresAtFormatted, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    if (diagnosticReport.tokenProviderClaim.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(AppStrings.idTokenProviderClaim(currentLang), color = TextSecondary, fontSize = 11.sp)
                                            Text(diagnosticReport.tokenProviderClaim, color = MetricBlue, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MetricRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(AppStrings.idTokenRetrievalFailed(currentLang), color = MetricRed, fontSize = 12.sp)
                                    }
                                }

                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))

                                // STAGE 2: Cloudflare Auth Verification
                                ProfileDataRow(
                                    label = AppStrings.cloudflareAuthStatus(currentLang),
                                    value = if (diagnosticReport.authOk) AppStrings.cloudflareAuthSucceeded(currentLang)
                                            else if (diagnosticReport.authExecuted) (diagnosticReport.authErrorMessage ?: "HTTP ${diagnosticReport.authHttpStatus}")
                                            else AppStrings.cloudflareNotExecuted(currentLang),
                                    valueColor = if (diagnosticReport.authOk) MetricGreen
                                                 else if (diagnosticReport.authExecuted) MetricRed
                                                 else TextSecondary
                                )

                                if (diagnosticReport.authExecuted && diagnosticReport.authProjectId != null) {
                                    ProfileDataRow(
                                        label = AppStrings.cloudflareVerifiedProject(currentLang),
                                        value = diagnosticReport.authProjectId ?: "",
                                        valueColor = MetricBlue
                                    )
                                }

                                if (diagnosticReport.authExecuted) {
                                    ProfileDataRow(
                                        label = AppStrings.cloudflareUidMatch(currentLang),
                                        value = if (diagnosticReport.authUidMatched) "Verified Match (UID Verified)" else "Mismatch / Unverified",
                                        valueColor = if (diagnosticReport.authUidMatched) MetricGreen else MetricRed
                                    )
                                }

                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))

                                // STAGE 3: Cloudflare Admin Authorization
                                ProfileDataRow(
                                    label = AppStrings.cloudflareAdminStatus(currentLang),
                                    value = if (diagnosticReport.adminAuthorized) AppStrings.cloudflareAdminSucceeded(currentLang)
                                            else if (diagnosticReport.adminExecuted) {
                                                if (diagnosticReport.adminHttpStatus == 403) AppStrings.cloudflareAdminDenied(currentLang)
                                                else (diagnosticReport.adminErrorMessage ?: "HTTP ${diagnosticReport.adminHttpStatus}")
                                            } else AppStrings.cloudflareNotExecuted(currentLang),
                                    valueColor = if (diagnosticReport.adminAuthorized) MetricGreen
                                                 else if (diagnosticReport.adminExecuted && diagnosticReport.adminHttpStatus == 403) WarningOrange
                                                 else if (diagnosticReport.adminExecuted) MetricRed
                                                 else TextSecondary
                                )

                                if (diagnosticReport.adminExecuted && diagnosticReport.adminRole != null) {
                                    ProfileDataRow(
                                        label = AppStrings.cloudflareVerifiedRole(currentLang),
                                        value = diagnosticReport.adminRole ?: "",
                                        valueColor = MetricBlue
                                    )
                                }

                                // OVERALL RESULT SUMMARY
                                val outcomeBorderColor = when (diagnosticReport.outcome) {
                                    is CloudflareDiagnosticResult.AuthVerifiedAdminVerified -> MetricGreen
                                    is CloudflareDiagnosticResult.AuthVerifiedAdminDenied,
                                    is CloudflareDiagnosticResult.AdminAccessDenied -> WarningOrange
                                    else -> MetricRed
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(8.dp),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(listOf(outcomeBorderColor, outcomeBorderColor))
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = AppStrings.diagnosticOutcomeLabel(currentLang),
                                            color = outcomeBorderColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = diagnosticReport.safeSummaryText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            // Security Warning Box regarding ID Token != Admin Authority
                            Text(
                                text = AppStrings.idTokenSecurityWarning(currentLang),
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )

                            // Action button: Verify Token & Cloudflare Gateway
                            CineStreamOutlinedLoadingButton(
                                onClick = {
                                    if (isInspectingToken) return@CineStreamOutlinedLoadingButton
                                    val user = currentUser
                                    if (user == null) {
                                        Toast.makeText(context, AppStrings.unauthenticated(currentLang), Toast.LENGTH_SHORT).show()
                                        return@CineStreamOutlinedLoadingButton
                                    }
                                    isInspectingToken = true
                                    coroutineScope.launch {
                                        try {
                                            val report = cloudflareAuthService.runFullDiagnostic(user)
                                            diagnosticReport = report
                                            when (report.outcome) {
                                                is CloudflareDiagnosticResult.AuthVerifiedAdminVerified -> {
                                                    Toast.makeText(context, AppStrings.cloudflareAdminSucceeded(currentLang), Toast.LENGTH_SHORT).show()
                                                }
                                                is CloudflareDiagnosticResult.AuthVerifiedAdminDenied,
                                                is CloudflareDiagnosticResult.AdminAccessDenied -> {
                                                    Toast.makeText(context, AppStrings.cloudflareAdminDenied(currentLang), Toast.LENGTH_SHORT).show()
                                                }
                                                else -> {
                                                    Toast.makeText(context, report.safeSummaryText.take(60), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            AppLogger.e("AdminProfileScreen", "Diagnostic execution error", e)
                                            diagnosticReport = CloudflareDiagnosticReport(
                                                hasExecuted = true,
                                                safeSummaryText = e.message ?: "Diagnostic failed"
                                            )
                                        } finally {
                                            isInspectingToken = false
                                        }
                                    }
                                },
                                isLoading = isInspectingToken,
                                text = if (diagnosticReport.hasExecuted) AppStrings.retryDiagnosticBtn(currentLang) else AppStrings.runFullDiagnosticBtn(currentLang),
                                leadingIcon = Icons.Default.CloudSync,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                borderColor = MetricBlue,
                                contentColor = MetricBlue
                            )
                        }
                    }
                }
            }

            // ========================================================
            // SECTION 4: Account Actions
            // ========================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Refresh Profile Information Button
                CineStreamOutlinedLoadingButton(
                    onClick = {
                        val user = currentUser
                        if (user == null) {
                            Toast.makeText(context, AppStrings.unauthenticated(currentLang), Toast.LENGTH_SHORT).show()
                            return@CineStreamOutlinedLoadingButton
                        }
                        isRefreshingProfile = true
                        coroutineScope.launch {
                            try {
                                user.reload().await()
                                currentUser = FirebaseAuth.getInstance().currentUser
                                evaluateAuthority(currentUser)
                                Toast.makeText(context, AppStrings.profileRefreshedMsg(currentLang), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                AppLogger.w("AdminProfileScreen", "Reload user notice: ${e.message}")
                                Toast.makeText(context, AppStrings.profileRefreshFailed(currentLang), Toast.LENGTH_SHORT).show()
                            } finally {
                                isRefreshingProfile = false
                            }
                        }
                    },
                    isLoading = isRefreshingProfile,
                    text = AppStrings.refreshProfileBtn(currentLang),
                    leadingIcon = Icons.Default.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    borderColor = DarkCardBorder,
                    contentColor = Color.White
                )

                // Optional Go To Settings Button
                if (onNavigateToSettings != null) {
                    CineStreamOutlinedLoadingButton(
                        onClick = onNavigateToSettings,
                        text = AppStrings.goToSettingsBtn(currentLang),
                        leadingIcon = Icons.Default.Settings,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        borderColor = DarkCardBorder,
                        contentColor = TextSecondary
                    )
                }

                // Sign Out Button
                CineStreamLoadingButton(
                    onClick = onLogoutClick,
                    containerColor = MetricRedBg,
                    contentColor = MetricRed,
                    leadingIcon = Icons.Default.Logout,
                    shape = RoundedCornerShape(12.dp),
                    text = AppStrings.logout(currentLang),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileDataRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
