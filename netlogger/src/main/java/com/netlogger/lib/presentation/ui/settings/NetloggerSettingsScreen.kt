package com.netlogger.lib.presentation.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netlogger.lib.R
import com.netlogger.lib.presentation.ui.detail.NetloggerDetailColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetloggerSettingsScreen(
    viewModel: NetloggerSettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Save or just back */ onBack() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = NetloggerDetailColors.Teal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Log Management
            SettingsSection(title = "Log Management") {
                SettingToggleItem(
                    title = "Auto-reset logs",
                    subtitle = "Clear all logs when app starts",
                    checked = settings.autoResetOnStart,
                    onCheckedChange = { viewModel.updateAutoReset(it) }
                )
            }

            // Shake to Report
            SettingsSection(title = "Shake to Report") {
                SettingToggleItem(
                    title = "Enable Shake Detector",
                    subtitle = "Shake device to instantly capture network state",
                    checked = settings.enableShakeDetector,
                    onCheckedChange = { viewModel.updateShakeDetector(it) }
                )
                
                if (settings.enableShakeDetector) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sensitivity", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Surface(
                                color = Color(0xFFE0F2F1),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                val label = when {
                                    settings.shakeSensitivity < 1.5f -> "LOW"
                                    settings.shakeSensitivity < 3.0f -> "MEDIUM"
                                    else -> "HIGH"
                                }
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00796B)
                                )
                            }
                        }
                        Slider(
                            value = settings.shakeSensitivity,
                            onValueChange = { viewModel.updateShakeSensitivity(it) },
                            valueRange = 1f..5f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00796B),
                                activeTrackColor = Color(0xFF00796B)
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Low", fontSize = 12.sp, color = Color.Gray)
                            Text("High", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // About
            SettingsSection(title = "About") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF00796B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_terminal),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("NetScanner Pro", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Version 1.0.0", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Powered by Clean Architecture & Koin dependency injection.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun NetloggerSettingsPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color(0xFFF8FAFC)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SettingsSection(title = "Log Management") {
                    SettingToggleItem(
                        title = "Auto-reset logs",
                        subtitle = "Clear all logs when app starts",
                        checked = true,
                        onCheckedChange = {}
                    )
                }

                SettingsSection(title = "Shake to Report") {
                    SettingToggleItem(
                        title = "Enable Shake Detector",
                        subtitle = "Shake device to instantly capture network state",
                        checked = true,
                        onCheckedChange = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sensitivity", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Surface(color = Color(0xFFE0F2F1), shape = RoundedCornerShape(4.dp)) {
                                Text("MEDIUM", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                            }
                        }
                        Slider(value = 0.5f, onValueChange = {}, colors = SliderDefaults.colors(thumbColor = Color(0xFF00796B), activeTrackColor = Color(0xFF00796B)))
                    }
                }

                SettingsSection(title = "About") {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF00796B)), contentAlignment = Alignment.Center) {
                            Icon(painter = painterResource(R.drawable.ic_terminal), contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("NetScanner Pro", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Version 1.0.0", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
