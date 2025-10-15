package com.example.skusho.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

/**
 * MIUI端末かどうかを判定
 */
private fun isMIUI(): Boolean {
    return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
           Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
           !Build.getRadioVersion().isNullOrEmpty() && Build.getRadioVersion().contains("MIUI")
}

private val PrimaryBlue = Color(0xFF1565C0)
private val SecondaryBlue = Color(0xFF0D47A1)
private val AccentBlue = Color(0xFF1E88E5)
private val PaleBlue = Color(0xFFE3F2FD)
private val SoftBlue = Color(0xFFF6F9FF)
private val MutedText = Color(0xFF5C6F82)
private val WarningContainer = Color(0xFFFFE9E9)
private val WarningContent = Color(0xFFB3261E)

private val OutlineButtonBorder = BorderStroke(1.dp, SecondaryBlue.copy(alpha = 0.5f))

@Composable
private fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = AccentBlue,
    contentColor = Color.White,
    disabledContainerColor = AccentBlue.copy(alpha = 0.3f),
    disabledContentColor = Color.White.copy(alpha = 0.7f)
)

@Composable
private fun secondaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = SecondaryBlue,
    contentColor = Color.White
)

@Composable
private fun outlineButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = SecondaryBlue
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 権限状態
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null
    
    // 表示するページを決定
    val isMiuiDevice = isMIUI()
    val pages = buildList {
        add(OnboardingPage.Welcome)
        
        // MIUI端末の場合はMIUI権限設定で一括設定するため、オーバーレイ権限ページは不要
        if (!isMiuiDevice) {
            add(OnboardingPage.OverlayPermission)
        }
        
        // MIUI端末の場合は、最重要のMIUI権限を先に設定
        if (isMiuiDevice) {
            add(OnboardingPage.MIUIPermission)
        }
        
        // 通知権限は補助的な機能なので後に配置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(OnboardingPage.NotificationPermission)
        }
        
        add(OnboardingPage.Ready)
    }
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    
    // 権限状態を監視
    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // メインコンテンツ
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            when (pages[pageIndex]) {
                OnboardingPage.Welcome -> WelcomePage()
                OnboardingPage.OverlayPermission -> OverlayPermissionPage(
                    hasPermission = hasOverlayPermission,
                    onRequestPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onCheckPermission = {
                        hasOverlayPermission = Settings.canDrawOverlays(context)
                    }
                )
                OnboardingPage.NotificationPermission -> NotificationPermissionPage(
                    permissionState = notificationPermissionState
                )
                OnboardingPage.MIUIPermission -> MIUIPermissionPage(
                    hasPermission = hasOverlayPermission,
                    onCheckPermission = {
                        hasOverlayPermission = Settings.canDrawOverlays(context)
                    }
                )
                OnboardingPage.Ready -> ReadyPage()
            }
        }
        
        // ページインジケーター
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (index == pagerState.currentPage) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage)
                                AccentBlue
                            else
                                SoftBlue
                        )
                )
            }
        }
        
        // ナビゲーションボタン
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 前へボタン
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    colors = outlineButtonColors(),
                    border = OutlineButtonBorder
                ) {
                    Text("前へ")
                }
            } else {
                Spacer(modifier = Modifier.size(0.dp))
            }
            
            // 次へ / 完了ボタン
            if (pagerState.currentPage < pages.size - 1) {
                // 現在のページの権限状態をチェック
                val canProceed = when (pages[pagerState.currentPage]) {
                    OnboardingPage.OverlayPermission -> hasOverlayPermission
                    OnboardingPage.NotificationPermission -> notificationPermissionState?.status?.isGranted ?: true
                    OnboardingPage.MIUIPermission -> hasOverlayPermission
                    else -> true // ようこそページと準備完了ページは常に進める
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    enabled = canProceed,
                    colors = primaryButtonColors()
                ) {
                    Text("次へ")
                }
            } else {
                Button(
                    onClick = onComplete,
                    colors = primaryButtonColors()
                ) {
                    Text("開始")
                }
            }
        }
    }
}

// ページの種類を定義
private enum class OnboardingPage {
    Welcome,
    OverlayPermission,
    NotificationPermission,
    MIUIPermission,
    Ready
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📸",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Skusho へようこそ",
            style = MaterialTheme.typography.headlineLarge,
            color = PrimaryBlue,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "画面端の撮影ボタンをタップするだけで\nスクリーンショットを撮影できます",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MutedText
        )
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = PaleBlue,
                contentColor = SecondaryBlue
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "✨ 主な機能",
                    style = MaterialTheme.typography.titleMedium,
                    color = SecondaryBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 他のアプリを使用中でも撮影可能\n• ドラッグ可能な撮影ボタン\n• 自動的にギャラリーに保存",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryBlue.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun OverlayPermissionPage(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onCheckPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎯",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "画面オーバーレイ権限",
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryBlue,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "撮影ボタンを表示するために\n「他のアプリの上に重ねて表示」の権限が必要です",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MutedText
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermission) PaleBlue else WarningContainer,
                contentColor = if (hasPermission) SecondaryBlue else WarningContent
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasPermission) "✅ 権限が許可されています" else "⚠️ 権限が必要です",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasPermission) SecondaryBlue else WarningContent
                )
                if (!hasPermission) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = primaryButtonColors()
                    ) {
                        Text("権限を許可")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onCheckPermission,
                        colors = outlineButtonColors(),
                        border = OutlineButtonBorder
                    ) {
                        Text("権限状態を再確認")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NotificationPermissionPage(
    permissionState: com.google.accompanist.permissions.PermissionState?
) {
    val hasPermission = permissionState?.status?.isGranted ?: true
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔔",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "通知権限",
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryBlue,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "サービスの状態確認と停止ボタンを\n通知バーに表示するために必要です",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MutedText
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermission) PaleBlue else WarningContainer,
                contentColor = if (hasPermission) SecondaryBlue else WarningContent
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasPermission) "✅ 権限が許可されています" else "⚠️ 権限が必要です",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasPermission) SecondaryBlue else WarningContent
                )
                if (!hasPermission && permissionState != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionState.launchPermissionRequest() },
                        colors = primaryButtonColors()
                    ) {
                        Text("権限を許可")
                    }
                }
            }
        }
    }
}

@Composable
private fun MIUIPermissionPage(
    hasPermission: Boolean,
    onCheckPermission: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚙️",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "MIUI 権限設定",
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryBlue,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "MIUI端末では専用の権限設定が必要です\n（オーバーレイ権限を含む）",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MutedText
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermission) PaleBlue else SoftBlue,
                contentColor = SecondaryBlue
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasPermission) {
                    Text(
                        text = "✅ 権限が許可されています",
                        style = MaterialTheme.typography.titleMedium,
                        color = SecondaryBlue
                    )
                } else {
                    Text(
                        text = "撮影ボタンの表示に必要な権限：",
                        style = MaterialTheme.typography.titleMedium,
                        color = SecondaryBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "✅ バックグラウンドで実行中に新しいウィンドウを開く\n✅ ポップアップウィンドウの表示",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryBlue.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ この2つを許可することで、オーバーレイ権限も有効になります",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryBlue.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "※ 他の項目（ホーム画面ショートカット、ロック画面に表示）は不要です",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryBlue.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = secondaryButtonColors()
                    ) {
                        Text("MIUI権限設定を開く")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onCheckPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlineButtonColors(),
                        border = OutlineButtonBorder
                    ) {
                        Text("権限状態を再確認")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "準備完了！",
            style = MaterialTheme.typography.headlineLarge,
            color = PrimaryBlue,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "設定は以上です。\n「開始」ボタンをタップして\nスクリーンショット撮影を始めましょう！",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MutedText
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SoftBlue,
                contentColor = SecondaryBlue
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📝 使い方",
                    style = MaterialTheme.typography.titleMedium,
                    color = SecondaryBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "1. 「撮影開始」をタップ\n2. 画面キャプチャの許可を承認\n3. 撮影ボタンが表示されます\n4. 撮影したい画面でボタンをタップ！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryBlue.copy(alpha = 0.85f)
                )
            }
        }
    }
}
