package com.yuhproducts.skusho.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Spacer(modifier = Modifier.height(16.dp))

            // 連写
            SettingSection(
                title = "${stringResource(R.string.continuous_shot)}: ${
                    if (uiState.continuousShotCount == 0) "OFF"
                    else "${uiState.continuousShotCount}枚"
                }"
            ) {
                Slider(
                    value = uiState.continuousShotCount.toFloat(),
                    onValueChange = { viewModel.onContinuousShotCountChanged(it.toInt()) },
                    valueRange = 0f..5f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "1回のタップで複数枚撮影します",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 連写速度
            SettingSection(
                title = "${stringResource(R.string.continuous_shot_speed)}: ${formatSpeed(uiState.continuousShotIntervalMs)}"
            ) {
                Slider(
                    value = speedToIndex(uiState.continuousShotIntervalMs).toFloat(),
                    onValueChange = { 
                        viewModel.onContinuousShotIntervalChanged(indexToSpeed(it.toInt())) 
                    },
                    valueRange = 0f..2f,  // 3段階
                    steps = 1,  // 0, 1, 2
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.continuousShotCount > 0  // 連写がONの時のみ有効
                )
                
                // 選択肢のラベル表示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "500ms\n${stringResource(R.string.speed_fast)}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "750ms\n${stringResource(R.string.speed_normal)}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "1.0秒\n${stringResource(R.string.speed_stable)}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                
                // 説明文
                Text(
                    text = when (uiState.continuousShotIntervalMs) {
                        500 -> "素早く連写します"
                        750 -> "バランスの取れた速度です"
                        1000 -> "確実に撮影します"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
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
    1000 -> "1.0秒"
    else -> "500ms"
}
