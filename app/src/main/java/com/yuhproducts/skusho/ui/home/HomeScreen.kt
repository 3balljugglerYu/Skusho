package com.yuhproducts.skusho.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.yuhproducts.skusho.R
import com.yuhproducts.skusho.service.CaptureService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.yuhproducts.skusho.util.TimeUtils

/**
 * MIUI端末かどうかを判定
 */
private fun isMIUI(): Boolean {
    return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
           Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
           !Build.getRadioVersion().isNullOrEmpty() && Build.getRadioVersion().contains("MIUI")
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onRestartTutorial: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // メニューの開閉状態
    var menuExpanded by remember { mutableStateOf(false) }
    
    // オーバーレイ権限の状態
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    
    // 通知権限の状態（Android 13+）
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    
    // 画面が表示されるたびに権限状態をチェック
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newOverlayPermission = Settings.canDrawOverlays(context)
                println("DEBUG: Overlay permission check - Current: $hasOverlayPermission, New: $newOverlayPermission")
                hasOverlayPermission = newOverlayPermission
                viewModel.checkServiceStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // MediaProjection の結果を受け取る
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                // サービスを開始
                val intent = Intent(context, CaptureService::class.java).apply {
                    putExtra(CaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(CaptureService.EXTRA_RESULT_DATA, data)
                }
                ContextCompat.startForegroundService(context, intent)
                
                // サービスが完全に起動するまで少し待ってから状態をチェック
                scope.launch {
                    delay(500) // サービス起動を待つ
                    viewModel.checkServiceStatus()
                }
            }
        } else {
            // ユーザーがキャンセルした場合
            viewModel.checkServiceStatus()
        }
    }
    
    // オーバーレイ権限設定画面への遷移
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }
    
    // MediaProjection の同意を求める
    LaunchedEffect(uiState.shouldRequestMediaProjection) {
        if (uiState.shouldRequestMediaProjection) {
            mediaProjectionLauncher.launch(viewModel.getMediaProjectionIntent())
            viewModel.onMediaProjectionRequested()
        }
    }
    
    // サービス状態の定期チェック（1秒ごと）
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.checkServiceStatus()
            delay(1000)
        }
    }
    
    val cardShape = RoundedCornerShape(24.dp)
    val scrollState = rememberScrollState()
    val primaryBlue = Color(0xFF1565C0)
    val secondaryBlue = Color(0xFF0D47A1)
    val accentBlue = Color(0xFF1E88E5)
    val paleBlue = Color(0xFFE3F2FD)
    val softBlue = Color(0xFFF5F9FF)

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = primaryBlue
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "メニュー",
                            tint = primaryBlue
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (isMIUI()) {
                            DropdownMenuItem(
                                text = { Text("MIUI権限設定を開く") },
                                onClick = {
                                    menuExpanded = false
                                    try {
                                        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                                            setClassName(
                                                "com.miui.securitycenter",
                                                "com.miui.permcenter.permissions.PermissionsEditorActivity"
                                            )
                                            putExtra("extra_pkgname", context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("オーバーレイ権限を開く") },
                                onClick = {
                                    menuExpanded = false
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            DropdownMenuItem(
                                text = { Text("通知権限を開く") },
                                onClick = {
                                    menuExpanded = false
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("チュートリアルを再確認") },
                            onClick = {
                                menuExpanded = false
                                onRestartTutorial()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = primaryBlue,
                    actionIconContentColor = primaryBlue
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = primaryBlue,
                    contentColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val rewardUnlockActive = uiState.isRewardUnlockActive
                        val canCapture = hasOverlayPermission &&
                            (notificationPermissionState == null || notificationPermissionState.status.isGranted) &&
                            rewardUnlockActive
                        val isCapturing = uiState.isServiceRunning
                        val statusIcon = when {
                            isCapturing -> Icons.Default.CheckCircle
                            canCapture -> Icons.Default.Info
                            else -> Icons.Default.Warning
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color.White
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val statusText = when {
                                isCapturing -> stringResource(R.string.status_capturing)
                                canCapture -> "撮影可能"
                                else -> "準備中"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            val supportingText = when {
                                isCapturing -> "カメラボタンをタップして撮影できます！"
                                canCapture -> "撮影開始ボタンをタップしてください！"
                                !hasOverlayPermission -> "必要な権限を許可してください"
                                notificationPermissionState != null && !notificationPermissionState.status.isGranted -> "通知の権限を許可してください"
                                !rewardUnlockActive -> stringResource(R.string.unlock_required)
                                else -> "撮影を開始する準備を進めてください"
                            }
                            Text(
                                text = supportingText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = paleBlue,
                    contentColor = secondaryBlue
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val primaryButtonColors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue,
                        contentColor = Color.White,
                        disabledContainerColor = secondaryBlue.copy(alpha = 0.2f),
                        disabledContentColor = secondaryBlue.copy(alpha = 0.4f)
                    )
                    val secondaryButtonColors = ButtonDefaults.buttonColors(
                        containerColor = secondaryBlue,
                        contentColor = Color.White
                    )
                    Text(
                        text = "クイック操作",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryBlue
                    )
                    Text(
                        text = "撮影サービスの開始・停止、設定画面への移動をまとめました。",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryBlue.copy(alpha = 0.75f)
                    )
                    if (!uiState.isServiceRunning) {
                        Button(
                            onClick = {
                                if (hasOverlayPermission) {
                                    viewModel.onStartCaptureClick()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasOverlayPermission &&
                                    (notificationPermissionState == null || notificationPermissionState.status.isGranted) &&
                                    uiState.isRewardUnlockActive,
                            colors = primaryButtonColors
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.start_capture),
                                color = Color.White
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.onStopCaptureClick() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = secondaryButtonColors
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.stop_capture),
                                color = Color.White
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = secondaryBlue),
                        border = BorderStroke(1.dp, secondaryBlue.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = secondaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings),
                            color = secondaryBlue
                        )
                    }
                }
            }

            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = softBlue,
                    contentColor = secondaryBlue
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val permissionButtonColors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue,
                        contentColor = Color.White,
                        disabledContainerColor = secondaryBlue.copy(alpha = 0.2f),
                        disabledContentColor = secondaryBlue.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "権限の状態",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryBlue
                    )
                    val overlayStatusColor = if (hasOverlayPermission) {
                        accentBlue
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    PermissionInfoRow(
                        icon = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                        label = "オーバーレイ表示",
                        status = if (hasOverlayPermission) "許可済み" else "未許可",
                        statusColor = overlayStatusColor,
                        supportingText = "本アプリ（Skusho）を選択し、権限を許可して下さい\n撮影ボタンを表示するために必要です"
                    )
                    if (!hasOverlayPermission && !isMIUI()) {
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                overlayPermissionLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = permissionButtonColors
                        ) {
                            Text(
                                text = stringResource(R.string.grant_permission),
                                color = Color.White
                            )
                        }
                    }

                    if (notificationPermissionState != null) {
                        val granted = notificationPermissionState.status.isGranted
                        val notificationColor = if (granted) {
                            accentBlue
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                        PermissionInfoRow(
                            icon = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            label = "通知",
                            status = if (granted) "許可済み" else "未許可",
                            statusColor = notificationColor,
                            supportingText = "キャプチャ状況を通知バーに表示します"
                        )
                        if (!granted) {
                            Button(
                                onClick = { notificationPermissionState.launchPermissionRequest() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = permissionButtonColors
                            ) {
                                Text(
                                    text = stringResource(R.string.grant_permission),
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        PermissionInfoRow(
                            icon = Icons.Default.Info,
                            label = "通知",
                            status = "不要",
                            statusColor = accentBlue,
                            supportingText = "Android 12以下では追加の権限は不要です"
                        )
                    }

                    Divider(color = secondaryBlue.copy(alpha = 0.1f))

                    val rewardUnlockActive = uiState.isRewardUnlockActive
                    val remainingMillis = uiState.rewardUnlockRemainingMillis
                    val unlockStatusColor = if (rewardUnlockActive) accentBlue else MaterialTheme.colorScheme.error
                    val remainingText = if (rewardUnlockActive) {
                        stringResource(
                            R.string.unlock_remaining_format,
                            TimeUtils.formatDurationMillis(remainingMillis)
                        )
                    } else {
                        stringResource(R.string.unlock_expired_notice)
                    }
                    val unlockLoadingMessage = stringResource(R.string.unlock_loading_message)
                    val unlockLoadingLabel = stringResource(R.string.unlock_loading)
                    val watchAdLabel = stringResource(R.string.watch_ad)
                    val watchAdDisabledLabel = stringResource(R.string.unlock_active_button_text)
                    val watchAdButtonEnabled = !uiState.isRewardAdLoading && !rewardUnlockActive

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.watch_ad_to_unlock),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryBlue
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (rewardUnlockActive) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = unlockStatusColor
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (rewardUnlockActive) {
                                        stringResource(R.string.unlock_active)
                                    } else {
                                        stringResource(R.string.unlock_inactive)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = unlockStatusColor
                                )
                                Text(
                                    text = remainingText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryBlue.copy(alpha = 0.75f)
                                )
                            }
                        }

                        val activity = (context as? Activity)
                        Button(
                            onClick = {
                                if (activity != null) {
                                    viewModel.onWatchAdClick(
                                        activity = activity,
                                        onAdUnavailable = {
                                            Toast.makeText(
                                                context,
                                                unlockLoadingMessage,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        unlockLoadingMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = watchAdButtonEnabled,
                            colors = permissionButtonColors
                        ) {
                            Text(
                                text = if (uiState.isRewardAdLoading) {
                                    unlockLoadingLabel
                                } else if (rewardUnlockActive) {
                                    watchAdDisabledLabel
                                } else {
                                    watchAdLabel
                                },
                                color = Color.White
                            )
                        }
                    }
                }
            }
            if (!hasOverlayPermission && isMIUI()) {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null
                            )
                            Text(
                                text = "MIUI端末で権限を有効にする",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "設定 → アプリ → アプリを管理 → Skusho → その他の権限 → 他のアプリの上に表示 を開き、権限を許可してください。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                                        setClassName(
                                            "com.miui.securitycenter",
                                            "com.miui.permcenter.permissions.PermissionsEditorActivity"
                                        )
                                        putExtra("extra_pkgname", context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text("MIUI権限設定を開く")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionInfoRow(
    icon: ImageVector,
    label: String,
    status: String,
    statusColor: Color,
    supportingText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(28.dp)
        )
        val contentColor = LocalContentColor.current
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
