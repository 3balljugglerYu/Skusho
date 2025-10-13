package com.example.skusho.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skusho.R
import com.example.skusho.service.CaptureService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * MIUI端末かどうかを判定
 */
private fun isMIUI(): Boolean {
    return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
           Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
           !Build.getRadioVersion().isNullOrEmpty() && Build.getRadioVersion().contains("MIUI")
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
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
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // タイトル
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ステータス表示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isServiceRunning) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uiState.isServiceRunning) {
                            stringResource(R.string.status_waiting)
                        } else {
                            "停止中"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.isServiceRunning) {
                            "浮遊ボタンをタップして撮影できます"
                        } else {
                            "撮影を開始してください"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // デバッグ情報表示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🐛 デバッグ情報",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "オーバーレイ権限: $hasOverlayPermission",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "サービス実行中: ${uiState.isServiceRunning}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "通知権限: ${notificationPermissionState?.status?.isGranted ?: "不要"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val newPermission = Settings.canDrawOverlays(context)
                            println("DEBUG: Manual check - Old: $hasOverlayPermission, New: $newPermission")
                            hasOverlayPermission = newPermission
                            viewModel.checkServiceStatus()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("手動で権限再チェック")
                    }
                    
                    // MIUI端末用の追加ガイド
                    if (!hasOverlayPermission && isMIUI()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ MIUI端末の場合",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "設定 → アプリ → アプリを管理 → Skusho → その他の権限 → 他のアプリの上に表示",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                                        setClassName("com.miui.securitycenter", 
                                                   "com.miui.permcenter.permissions.PermissionsEditorActivity")
                                        putExtra("extra_pkgname", context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // フォールバック: 通常のアプリ設定画面
                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("MIUI権限設定を開く")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 権限チェック表示
            if (!hasOverlayPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.permission_overlay_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                overlayPermissionLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.grant_permission))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (notificationPermissionState != null && !notificationPermissionState.status.isGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.permission_notification_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                notificationPermissionState.launchPermissionRequest()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.grant_permission))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // メインボタン
            if (!uiState.isServiceRunning) {
                Button(
                    onClick = {
                        if (hasOverlayPermission) {
                            viewModel.onStartCaptureClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasOverlayPermission &&
                            (notificationPermissionState == null || notificationPermissionState.status.isGranted)
                ) {
                    Text(stringResource(R.string.start_capture))
                }
            } else {
                Button(
                    onClick = {
                        viewModel.onStopCaptureClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.stop_capture))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 設定ボタン
            OutlinedButton(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings))
            }
        }
    }
}

