package com.example.skusho.ui.settings

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skusho.R

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
            // 画像形式
            SettingSection(title = stringResource(R.string.image_format)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.imageFormat == "PNG",
                        onClick = { viewModel.onImageFormatChanged("PNG") }
                    )
                    Text("PNG", modifier = Modifier.padding(start = 8.dp))
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    RadioButton(
                        selected = uiState.imageFormat == "JPEG",
                        onClick = { viewModel.onImageFormatChanged("JPEG") }
                    )
                    Text("JPEG", modifier = Modifier.padding(start = 8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 画像品質（JPEG の場合のみ）
            if (uiState.imageFormat == "JPEG") {
                SettingSection(
                    title = "画像品質: ${uiState.imageQuality}%"
                ) {
                    Slider(
                        value = uiState.imageQuality.toFloat(),
                        onValueChange = { viewModel.onImageQualityChanged(it.toInt()) },
                        valueRange = 50f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "低品質 ← → 高品質",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 撮影音
            SettingSection(title = stringResource(R.string.capture_sound)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.captureSoundEnabled) "ON" else "OFF",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = uiState.captureSoundEnabled,
                        onCheckedChange = { viewModel.onCaptureSoundChanged(it) }
                    )
                }
                Text(
                    text = "シャッター音を再生します",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
                    steps = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "1回のタップで複数枚撮影します（300ms間隔）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

