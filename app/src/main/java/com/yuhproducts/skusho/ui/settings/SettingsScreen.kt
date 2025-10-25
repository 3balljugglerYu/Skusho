package com.yuhproducts.skusho.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yuhproducts.skusho.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // HomeScreenと同じカラーパレット
    val primaryBlue = Color(0xFF1565C0)
    val secondaryBlue = Color(0xFF0D47A1)
    val accentBlue = Color(0xFF1E88E5)
    val paleBlue = Color(0xFFE3F2FD)
    val softBlue = Color(0xFFF5F9FF)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings),
                        color = primaryBlue,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "戻る",
                            tint = primaryBlue
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = primaryBlue,
                    navigationIconContentColor = primaryBlue
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Spacer(modifier = Modifier.height(16.dp))

            // 連写
            SettingSection(
                title = "${stringResource(R.string.continuous_shot)}: ${
                    if (uiState.continuousShotCount == 0) "OFF"
                    else "${uiState.continuousShotCount}枚"
                }",
                primaryBlue = primaryBlue,
                secondaryBlue = secondaryBlue,
                accentBlue = accentBlue,
                paleBlue = paleBlue
            ) {
                // モダンなセグメントコントロール
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // セグメントボタン
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(secondaryBlue.copy(alpha = 0.1f))
                            .padding(4.dp)
                    ) {
                        listOf("OFF", "2", "3", "4", "5").forEachIndexed { index, label ->
                            val shotCount = if (index == 0) 0 else index + 1  // OFF=0, 2=2, 3=3, 4=4, 5=5
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (uiState.continuousShotCount == shotCount) {
                                            accentBlue
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable { viewModel.onContinuousShotCountChanged(shotCount) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (uiState.continuousShotCount == shotCount) {
                                        Color.White
                                    } else {
                                        secondaryBlue
                                    }
                                )
                            }
                        }
                    }
                    
                    // 説明文
                    Text(
                        text = "1回のタップで複数枚撮影します",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryBlue.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 連写速度
            SettingSection(
                title = "${stringResource(R.string.continuous_shot_speed)}: ${formatSpeed(uiState.continuousShotIntervalMs)}",
                primaryBlue = primaryBlue,
                secondaryBlue = secondaryBlue,
                accentBlue = accentBlue,
                paleBlue = paleBlue
            ) {
                // モダンなセグメントコントロール
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // セグメントボタン
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (uiState.continuousShotCount > 0) {
                                    secondaryBlue.copy(alpha = 0.1f)
                                } else {
                                    secondaryBlue.copy(alpha = 0.05f)
                                }
                            )
                            .padding(4.dp)
                    ) {
                        listOf(
                            Pair(stringResource(R.string.speed_fast), 500),
                            Pair(stringResource(R.string.speed_normal), 750),
                            Pair(stringResource(R.string.speed_stable), 1000)
                        ).forEach { (label, value) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (uiState.continuousShotIntervalMs == value) {
                                            accentBlue
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable(
                                        enabled = uiState.continuousShotCount > 0
                                    ) { 
                                        viewModel.onContinuousShotIntervalChanged(value) 
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.continuousShotIntervalMs == value) {
                                        Color.White
                                    } else {
                                        secondaryBlue.copy(
                                            alpha = if (uiState.continuousShotCount > 0) 0.7f else 0.3f
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    // 説明文
                    Text(
                        text = when (uiState.continuousShotIntervalMs) {
                            500 -> "約0.5秒の間隔で連写します"
                            750 -> "約0.75秒の間隔で連写します"
                            1000 -> "約1秒の間隔で連写します"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryBlue.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    primaryBlue: Color,
    secondaryBlue: Color,
    accentBlue: Color,
    paleBlue: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = paleBlue,
            contentColor = secondaryBlue
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = secondaryBlue
            )
            content()
        }
    }
}

// インデックス → ミリ秒
private fun indexToSpeed(index: Int): Int = when (index) {
    0 -> 500
    1 -> 750
    2 -> 1000
    else -> 500
}

// ミリ秒 → インデックス
private fun speedToIndex(speedMs: Int): Int = when (speedMs) {
    500 -> 0
    750 -> 1
    1000 -> 2
    else -> 0
}

// 表示用フォーマット
private fun formatSpeed(speedMs: Int): String = when (speedMs) {
    500 -> "500ms"
    750 -> "750ms"
    1000 -> "1.0s"
    else -> "500ms"
}
