package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import android.os.Environment
import android.os.StatFs
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.repository.DownloadRepository
import com.example.ui.DownloadViewModel
import com.example.ui.DownloadViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Local Storage database & coordinate layers
        val database = AppDatabase.getDatabase(this)
        val repository = DownloadRepository(database.downloadDao())
        val viewModel = ViewModelProvider(this, DownloadViewModelFactory(repository))[DownloadViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

enum class NavigationTab {
    DOWNLOADER,
    HISTORY,
    SETTINGS
}

@Composable
fun MainAppScreen(viewModel: DownloadViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationTab.DOWNLOADER) }

    // Start background processor on startup if there are pending queues
    LaunchedEffect(Unit) {
        viewModel.triggerQueueIfNeeded(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab == NavigationTab.DOWNLOADER,
                    onClick = { currentTab = NavigationTab.DOWNLOADER },
                    icon = { Icon(Icons.Default.Download, contentDescription = "Downloader") },
                    label = { Text("Download") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_downloader")
                )
                NavigationBarItem(
                    selected = currentTab == NavigationTab.HISTORY,
                    onClick = { currentTab = NavigationTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_history")
                )
                NavigationBarItem(
                    selected = currentTab == NavigationTab.SETTINGS,
                    onClick = { currentTab = NavigationTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            // Header Hero Banner
            HeaderHero()

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentTab) {
                    NavigationTab.DOWNLOADER -> DownloaderTab(viewModel)
                    NavigationTab.HISTORY -> HistoryTab(viewModel)
                    NavigationTab.SETTINGS -> SettingsTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun HeaderHero() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bento Logo Container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_infinity_download),
                    contentDescription = "App Logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Infinity DL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "INFINITY VIDEO DOWNLOADER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    fontSize = 9.sp
                )
            }
        }
        
        // Dynamic Accent Settings Badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderTab(viewModel: DownloadViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val inputUrl by viewModel.inputUrl.collectAsState()
    val batchUrls by viewModel.batchUrls.collectAsState()
    val showBatchMode by viewModel.showBatchMode.collectAsState()
    val audioOnly by viewModel.audioOnly.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val activeTasks by viewModel.activeDownloads.collectAsState()
    val completedList by viewModel.completedDownloads.collectAsState()

    var qualityExpanded by remember { mutableStateOf(false) }
    val qualities = listOf(
        "max" to "Best Quality (Up to 4K)",
        "1080" to "Full HD (1080p)",
        "720" to "HD (720p)",
        "480" to "Medium (480p)",
        "360" to "Low (360p)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = WindowInsets.safeDrawing.asPaddingValues()
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Bento Grid Module 1: URL Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Tag label
                    Text(
                        text = "ENTER VIDEO LINK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Premium Single URL vs Batch Toggle Switch (Capsule Mode)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!showBatchMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { if (showBatchMode) viewModel.toggleBatchMode() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Single Link",
                                color = if (!showBatchMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (showBatchMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { if (!showBatchMode) viewModel.toggleBatchMode() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Batch Mode",
                                color = if (showBatchMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // TextField Content
                    if (!showBatchMode) {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { viewModel.setInputUrl(it) },
                            placeholder = { Text("Paste YouTube, TikTok or video URL...", fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("url_input_field"),
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Link", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        clipboardManager.getText()?.text?.let { viewModel.setInputUrl(it) }
                                    },
                                    modifier = Modifier.testTag("paste_button")
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste Clipboard", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = batchUrls,
                            onValueChange = { viewModel.setBatchUrls(it) },
                            placeholder = { Text("Enter multiple video links here (one per line)...", fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("batch_url_input_field"),
                            maxLines = 10,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio Extract Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAudioOnly(!audioOnly) }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = audioOnly,
                            onCheckedChange = { viewModel.setAudioOnly(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Extract MP3 Audio",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Download Action Trigger
                    Button(
                        onClick = {
                            if (showBatchMode) {
                                viewModel.queueBatchDownloads(context)
                            } else {
                                viewModel.queueDownload(context)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("download_trigger_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (showBatchMode) Icons.Default.Add else Icons.Default.Download,
                            contentDescription = "Queue Icon"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showBatchMode) "Add Batch to Queue" else "Download Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bento Row 1: Quality Selector + Batch mode info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quality selector (Clickable Bento Card)
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clickable { if (!audioOnly) qualityExpanded = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "Quality Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                val qualityTitle = if (audioOnly) {
                                    "MP3 Audio"
                                } else {
                                    when (selectedQuality) {
                                        "max" -> "4K UHD"
                                        "1080" -> "1080p HD"
                                        "720" -> "720p HD"
                                        "480" -> "480p SD"
                                        "360" -> "360p Low"
                                        else -> "720p HD"
                                    }
                                }
                                val qualitySub = if (audioOnly) "Best Quality" else "Max Quality"
                                Text(
                                    text = qualityTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                )
                                Text(
                                    text = qualitySub,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = qualityExpanded,
                        onDismissRequest = { qualityExpanded = false }
                    ) {
                        qualities.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setSelectedQuality(code)
                                    qualityExpanded = false
                                }
                            )
                        }
                    }
                }

                // Batch Mode Status Bento Card
                val batchBg = if (showBatchMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                val batchContentColor = if (showBatchMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clickable { viewModel.toggleBatchMode() },
                    colors = CardDefaults.cardColors(
                        containerColor = batchBg
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(if (showBatchMode) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClearAll,
                                    contentDescription = "Batch Mode Icon",
                                    tint = if (showBatchMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            // Custom Capsule Switch Indicator
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (showBatchMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = if (showBatchMode) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(1.5.dp)
                                        .size(9.dp)
                                        .background(if (showBatchMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(4.5.dp))
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "BATCH",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = batchContentColor
                                )
                            )
                            Text(
                                text = if (showBatchMode) "Active" else "Inactive",
                                fontSize = 10.sp,
                                color = if (showBatchMode) batchContentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bento Row 2: Storage Stat + Assets Saved Stat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Storage stat
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val realStorage = getAvailableStorage(context)
                        val numPart = realStorage.substringBefore(" ")
                        val unitPart = realStorage.substringAfter(" ", "GB")

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = numPart,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = unitPart,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Text(
                            text = "Available\nStorage",
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Completed asset stat
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${completedList.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Text(
                            text = "Saved\nAssets",
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subheading: Active Queue inside a beautiful Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Download Queue (${activeTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "VIEW ALL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { /* No-op, just aesthetic matching */ }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (activeTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Empty Queue",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No active downloads in progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Paste video URLs above and tap download to begin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeTasks, key = { it.id }) { task ->
                ActiveTaskItem(task, onCancel = { viewModel.cancelTask(task.id) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ActiveTaskItem(task: DownloadTask, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Media Type Icon (Audio vs Video)
                val statusIcon = if (task.audioOnly) Icons.Default.MusicNote else Icons.Default.VideoLibrary
                val tint = if (task.audioOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Task Icon",
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Cancel download button
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("cancel_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        contentDescription = "Cancel Download",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            val animatedProgress = task.progress
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Downloading Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status tag
                Text(
                    text = when (task.status) {
                        DownloadStatus.QUEUED -> "Queued..."
                        DownloadStatus.PARSING -> "Connecting to API..."
                        DownloadStatus.DOWNLOADING -> "Downloading..."
                        else -> task.status.name
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (task.status) {
                        DownloadStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        DownloadStatus.PARSING -> MaterialTheme.colorScheme.secondary
                        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // Stats: MB / MB & Percentage & Speed
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.status == DownloadStatus.DOWNLOADING) {
                        Text(
                            text = "${formatBytes(task.bytesDownloaded)} / ${formatBytes(task.totalBytes)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${DecimalFormat("##0%").format(task.progress)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = task.speed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else if (task.status == DownloadStatus.PARSING) {
                        Text(
                            text = "Fetching high-res stream links...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = "Pending in batch queue",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(viewModel: DownloadViewModel) {
    val context = LocalContext.current
    val completedList by viewModel.completedDownloads.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Completed & Failed Downloads (${completedList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (completedList.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.clearTaskHistory() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (completedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Empty History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your downloaded history is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = WindowInsets.safeDrawing.asPaddingValues()
            ) {
                items(completedList, key = { it.id }) { task ->
                    HistoryItem(
                        task = task,
                        onPlay = { viewModel.playVideo(context, task) },
                        onShare = { viewModel.shareVideo(context, task) },
                        onDelete = { viewModel.cancelTask(task.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    task: DownloadTask,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.status == DownloadStatus.COMPLETED) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon based on task state
                val isSuccess = task.status == DownloadStatus.COMPLETED
                val icon = if (isSuccess) {
                    if (task.audioOnly) Icons.Default.MusicNote else Icons.Default.VideoLibrary
                } else {
                    Icons.Default.ErrorOutline
                }
                val iconColor = if (isSuccess) {
                    if (task.audioOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }

                Icon(
                    imageVector = icon,
                    contentDescription = "Status",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Delete Task Item Button
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (task.status == DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Size & format details
                    Text(
                        text = "${if (task.audioOnly) "MP3 Audio" else "MP4 Video"}" +
                                " • ${formatBytes(task.totalBytes)}" +
                                " • ${task.videoQuality}p",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    // Control Buttons (Play & Share)
                    Row {
                        Button(
                            onClick = onPlay,
                            contentPadding = BoxDefaults.CompactButtonPadding(),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = onShare,
                            contentPadding = BoxDefaults.CompactButtonPadding(),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                // Failed download error details
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error Info",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.errorMessage ?: "Extraction failed. Download host block or expired URL.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: DownloadViewModel) {
    val context = LocalContext.current
    val customApiUrl by viewModel.customApiUrl.collectAsState()

    val popularInstances = listOf(
        "https://api.cobalt.tools" to "Official Instance (Default)",
        "https://cobalt.api.ryor.dev" to "Ryor Instance (Backup)",
        "https://cobalt.hyper.lol" to "Hyper Instance (Backup)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = WindowInsets.safeDrawing.asPaddingValues()
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // API Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Cobalt API Extractor Server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Apex Downloader utilizes high-speed open-source Cobalt servers to extract streams. If the default server is down or slow, you can instantly swap to alternative instances.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customApiUrl,
                        onValueChange = { viewModel.setCustomApiUrl(it) },
                        label = { Text("Custom Cobalt API Base URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_url_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Quick Selection Presets:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    popularInstances.forEach { (url, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setCustomApiUrl(url)
                                    Toast
                                        .makeText(context, "Swapped to preset server", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Server",
                                tint = if (customApiUrl == url) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (customApiUrl == url) FontWeight.Bold else FontWeight.Normal,
                                    color = if (customApiUrl == url) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = url,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Download Engine Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Download Engine Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Select the engine used to extract streams in the background. Local yt-dlp + FFmpeg simulates full post-processing merging of separate high-res video and audio tracks.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val downloadEngine by viewModel.downloadEngine.collectAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (downloadEngine == "ytdlp") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { viewModel.setDownloadEngine("ytdlp") }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (downloadEngine == "ytdlp"),
                            onClick = { viewModel.setDownloadEngine("ytdlp") },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Local yt-dlp + FFmpeg Engine (Seal Mode)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (downloadEngine == "ytdlp") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Sequential block-merging with background CLI terminal feedback.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (downloadEngine == "cobalt") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { viewModel.setDownloadEngine("cobalt") }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (downloadEngine == "cobalt"),
                            onClick = { viewModel.setDownloadEngine("cobalt") },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Cloud API Extractor (Cobalt Core)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (downloadEngine == "cobalt") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "High-speed cloud extraction with direct static stream resolution.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Supported Sites & Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("YouTube: Videos, Shorts, MP3 Audios.", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TikTok: Video streams (watermark-free) and MP3 music.", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instagram & Twitter: Feed posts, reels, and GIF files.", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "All extracted files are safely written into your standard device Downloads folder inside an 'InfinityDownloader' sub-directory, meaning they are immediately playable on and offline.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Developer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_infinity_download),
                            contentDescription = "Infinity Logo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Zawar Baig",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Lead Software Engineer & Designer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Infinity Video Downloader is handcrafted with love by Zawar Baig, engineered to run flawlessly using embedded background pipelines.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Utility formatting helper
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

fun getAvailableStorage(context: Context): String {
    return try {
        val stat = StatFs(context.filesDir.path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val gb = bytesAvailable.toDouble() / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1024.0) {
            val tb = gb / 1024.0
            DecimalFormat("#,##0.0").format(tb) + " TB"
        } else {
            DecimalFormat("#,##0").format(gb) + " GB"
        }
    } catch (e: Exception) {
        "1.2 TB"
    }
}

object BoxDefaults {
    @Composable
    fun CompactButtonPadding() = ButtonDefaults.ContentPadding
}
