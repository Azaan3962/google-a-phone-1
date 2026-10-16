package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ApkInfo
import com.example.model.DownloadPreset
import com.example.model.InstalledAppInfo
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GreenSuccess
import com.example.util.ApkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApkInstallerScreen(
  onNavigateToGoLoginWeb: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedTab by remember { mutableStateOf(0) } // 0: APK Files & Installer, 1: Installed Apps / Play Apps
  var selectedApk by remember { mutableStateOf<ApkInfo?>(null) }
  var storedApks by remember { mutableStateOf<List<ApkInfo>>(emptyList()) }
  var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
  var searchQuery by remember { mutableStateOf("") }
  var isProcessingUpload by remember { mutableStateOf(false) }

  // Downloader state
  var downloadUrlInput by remember { mutableStateOf("") }
  var isDownloading by remember { mutableStateOf(false) }
  var downloadProgress by remember { mutableStateOf(0) }
  var showDownloadDialog by remember { mutableStateOf(false) }

  // Permission dialog
  var showInstallPermissionDialog by remember { mutableStateOf(false) }

  // Load stored APKs & installed apps
  fun reloadData() {
    coroutineScope.launch {
      val apks = withContext(Dispatchers.IO) {
        ApkManager.getAllStoredApks(context)
      }
      storedApks = apks
      if (selectedApk == null && apks.isNotEmpty()) {
        selectedApk = apks.first()
      } else if (selectedApk != null) {
        // update selected APK status
        val currentFile = File(selectedApk!!.filePath)
        if (currentFile.exists()) {
          selectedApk = ApkManager.extractApkInfo(context, currentFile)
        }
      }

      val apps = withContext(Dispatchers.IO) {
        ApkManager.getInstalledApps(context, onlyUserApps = false)
      }
      installedApps = apps
    }
  }

  LaunchedEffect(Unit) {
    reloadData()
  }

  // File Picker for APKs
  val apkPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      coroutineScope.launch {
        isProcessingUpload = true
        try {
          val savedFile = ApkManager.saveApkFromUri(context, uri)
          if (savedFile != null) {
            val extracted = ApkManager.extractApkInfo(context, savedFile)
            if (extracted != null) {
              selectedApk = extracted
              Toast.makeText(context, "Loaded APK: ${extracted.appName}", Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(context, "Saved APK file: ${savedFile.name}", Toast.LENGTH_SHORT).show()
            }
            reloadData()
          } else {
            Toast.makeText(context, "Could not process selected file", Toast.LENGTH_LONG).show()
          }
        } catch (e: Exception) {
          Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
          isProcessingUpload = false
        }
      }
    }
  }

  // Pre-configured download presets
  val presets = remember {
    listOf(
      DownloadPreset(
        id = "gologin_official",
        title = "Official GoLogin APK",
        category = "Official Antidetect",
        description = "GoLogin multi-profile mobile browser client for Android",
        directDownloadUrl = "https://gologin.com/download-android",
        officialSiteUrl = "https://gologin.com",
        fileName = "GoLogin_Official.apk",
        isOfficialGoLogin = true
      ),
      DownloadPreset(
        id = "orbita_browser",
        title = "Orbita Chromium Core",
        category = "Isolated Browser",
        description = "Anti-detect Chromium engine with customized canvas & fingerprint protection",
        directDownloadUrl = "https://gologin.com/orbita-browser",
        officialSiteUrl = "https://gologin.com",
        fileName = "Orbita_Browser.apk",
        isOfficialGoLogin = true
      ),
      DownloadPreset(
        id = "chromium_browser",
        title = "Chromium Test Engine",
        category = "Open Source Browser",
        description = "Standalone lightweight browser APK for testing proxy & WebRTC isolation",
        directDownloadUrl = "https://download-chromium.appspot.com/dl/Android?type=snapshots",
        officialSiteUrl = "https://www.chromium.org",
        fileName = "Chromium_Node.apk",
        isOfficialGoLogin = false
      )
    )
  }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
      ) {
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.primary
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("APK Installer & Runner", fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            modifier = Modifier.testTag("tab_apk_installer")
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Play Installed Apps (${installedApps.size})", fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
            modifier = Modifier.testTag("tab_installed_apps")
          )
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      if (selectedTab == 0) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
          // Upload & File Picker Hero Card
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Box(
                    modifier = Modifier
                      .size(44.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.CloudUpload,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Install & Play APK Files",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "Upload any APK to inspect, install, or run on this phone",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = {
                      apkPickerLauncher.launch("application/vnd.android.package-archive")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .testTag("upload_apk_button")
                  ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select APK File", fontWeight = FontWeight.Bold)
                  }

                  OutlinedButton(
                    onClick = { showDownloadDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .height(48.dp)
                      .testTag("download_url_button")
                  ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Direct URL")
                  }
                }

                if (isProcessingUpload) {
                  Spacer(modifier = Modifier.height(12.dp))
                  LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                  Text(
                    text = "Analyzing package headers...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                  )
                }
              }
            }
          }

          // Active Selected APK Detailed Inspector Card
          if (selectedApk != null) {
            item {
              val apk = selectedApk!!
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (apk.isInstalled) GreenSuccess else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    if (apk.icon != null) {
                      Image(
                        bitmap = apk.icon.asImageBitmap(),
                        contentDescription = apk.appName,
                        modifier = Modifier
                          .size(56.dp)
                          .clip(RoundedCornerShape(12.dp))
                      )
                    } else {
                      Box(
                        modifier = Modifier
                          .size(56.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          Icons.Default.Android,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier = Modifier.size(32.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = apk.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = apk.packageName,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = "v${apk.versionName} (${apk.versionCode})",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "• ${(apk.fileSizeBytes / (1024 * 1024.0)).format(1)} MB",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // Status Indicator
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    if (apk.isInstalled) {
                      StatusBadge(
                        text = "INSTALLED ON THIS PHONE (v${apk.installedVersionName ?: apk.versionName})",
                        isPositive = true,
                        icon = Icons.Default.CheckCircle
                      )
                    } else {
                      StatusBadge(
                        text = "READY TO INSTALL",
                        isPositive = false,
                        icon = Icons.Default.Info
                      )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                      text = "SDK ${apk.minSdk}+ (Target: ${apk.targetSdk})",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  Spacer(modifier = Modifier.height(16.dp))

                  // ACTION BUTTONS: Install & Play
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    // Install Button
                    Button(
                      onClick = {
                        val hasPerm = ApkManager.canRequestPackageInstalls(context)
                        if (!hasPerm) {
                          showInstallPermissionDialog = true
                        } else {
                          val res = ApkManager.installApk(context, File(apk.filePath))
                          if (res.isFailure) {
                            Toast.makeText(
                              context,
                              "Install error: ${res.exceptionOrNull()?.message}",
                              Toast.LENGTH_LONG
                            ).show()
                          } else {
                            Toast.makeText(context, "Opening Android Package Installer...", Toast.LENGTH_SHORT).show()
                          }
                        }
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = if (apk.isInstalled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                        contentColor = if (apk.isInstalled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                      ),
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("install_apk_button")
                    ) {
                      Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(if (apk.isInstalled) "Reinstall / Update" else "Install APK", fontWeight = FontWeight.Bold)
                    }

                    // Play / Launch Button (ENABLED when app is installed!)
                    Button(
                      onClick = {
                        if (apk.isInstalled) {
                          val launched = ApkManager.launchApp(context, apk.packageName)
                          if (!launched) {
                            Toast.makeText(context, "Could not launch ${apk.appName}", Toast.LENGTH_SHORT).show()
                          }
                        } else {
                          // Prompt user to install first
                          Toast.makeText(context, "Please install ${apk.appName} first to play it!", Toast.LENGTH_SHORT).show()
                          val res = ApkManager.installApk(context, File(apk.filePath))
                          if (res.isFailure) {
                            showInstallPermissionDialog = true
                          }
                        }
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = if (apk.isInstalled) GreenSuccess else Color(0xFF1E293B),
                        contentColor = Color.White
                      ),
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("play_apk_button")
                    ) {
                      Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(if (apk.isInstalled) "Play / Launch" else "Install to Play", fontWeight = FontWeight.Bold)
                    }
                  }

                  // Secondary Actions (Share, Delete, Refresh status)
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                  ) {
                    TextButton(
                      onClick = {
                        val file = File(apk.filePath)
                        if (file.exists()) {
                          val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                          )
                          val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/vnd.android.package-archive"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                          }
                          context.startActivity(Intent.createChooser(shareIntent, "Share APK"))
                        }
                      }
                    ) {
                      Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Share APK", fontSize = 12.sp)
                    }

                    TextButton(
                      onClick = { reloadData() }
                    ) {
                      Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Check Status", fontSize = 12.sp)
                    }

                    TextButton(
                      onClick = {
                        ApkManager.deleteApk(File(apk.filePath))
                        selectedApk = null
                        reloadData()
                        Toast.makeText(context, "APK deleted", Toast.LENGTH_SHORT).show()
                      },
                      colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Remove", fontSize = 12.sp)
                    }
                  }

                  // Permissions preview
                  if (apk.permissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      text = "Requested Permissions (${apk.permissions.size}):",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      apk.permissions.take(6).forEach { perm ->
                        val shortName = perm.substringAfterLast(".")
                        SuggestionChip(
                          onClick = {},
                          label = { Text(shortName, fontSize = 10.sp) },
                          colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                          )
                        )
                      }
                      if (apk.permissions.size > 6) {
                        SuggestionChip(
                          onClick = {},
                          label = { Text("+${apk.permissions.size - 6} more", fontSize = 10.sp) }
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // Stored APKs vault
          if (storedApks.isNotEmpty()) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Saved APK Vault (${storedApks.size})",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { reloadData() }) {
                  Text("Refresh")
                }
              }
            }

            items(storedApks) { apk ->
              val isCurrent = selectedApk?.filePath == apk.filePath
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedApk = apk }
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (apk.icon != null) {
                    Image(
                      bitmap = apk.icon.asImageBitmap(),
                      contentDescription = apk.appName,
                      modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                    )
                  } else {
                    Icon(
                      Icons.Default.Android,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(40.dp)
                    )
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = apk.appName,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${apk.fileName} • ${(apk.fileSizeBytes / (1024 * 1024.0)).format(1)} MB",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  if (apk.isInstalled) {
                    IconButton(
                      onClick = { ApkManager.launchApp(context, apk.packageName) }
                    ) {
                      Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = GreenSuccess)
                    }
                  } else {
                    IconButton(
                      onClick = {
                        val res = ApkManager.installApk(context, File(apk.filePath))
                        if (res.isFailure) showInstallPermissionDialog = true
                      }
                    ) {
                      Icon(Icons.Default.DownloadForOffline, contentDescription = "Install", tint = MaterialTheme.colorScheme.primary)
                    }
                  }
                }
              }
            }
          }

          // GoLogin & Browser Download Catalog
          item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Official GoLogin Software & Core APKs",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Download official GoLogin clients and browsers to run on this phone separately",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          items(presets) { preset ->
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(
                        if (preset.isOfficialGoLogin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = if (preset.isOfficialGoLogin) Icons.Default.Security else Icons.Default.Language,
                      contentDescription = null,
                      tint = if (preset.isOfficialGoLogin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                      )
                      if (preset.isOfficialGoLogin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(text = "OFFICIAL", isPositive = true)
                      }
                    }
                    Text(
                      text = preset.category,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = preset.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = {
                      downloadUrlInput = preset.directDownloadUrl
                      showDownloadDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Get APK", fontSize = 13.sp)
                  }

                  OutlinedButton(
                    onClick = {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(preset.officialSiteUrl))
                      context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Official Site", fontSize = 13.sp)
                  }
                }
              }
            }
          }
        }
      } else {
        // TAB 1: INSTALLED APPS / PLAY APPS
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
        ) {
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps to play...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = null)
                }
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(12.dp))

          val filtered = remember(installedApps, searchQuery) {
            if (searchQuery.isBlank()) {
              installedApps
            } else {
              installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                  it.packageName.contains(searchQuery, ignoreCase = true)
              }
            }
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
          ) {
            item {
              Text(
                text = "Launch & Play Installed Apps (${filtered.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            items(filtered) { app ->
              val isGoLoginApp = app.packageName.contains("gologin", ignoreCase = true)
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isGoLoginApp) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(44.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = if (isGoLoginApp) Icons.Default.Security else Icons.Default.Android,
                      contentDescription = null,
                      tint = if (isGoLoginApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      if (isGoLoginApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(text = "GOLOGIN", isPositive = true)
                      }
                    }
                    Text(
                      text = app.packageName,
                      style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "v${app.versionName}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }

                  // 1-Click "Play / Launch" Button!
                  Button(
                    onClick = {
                      val success = ApkManager.launchApp(context, app.packageName)
                      if (!success) {
                        Toast.makeText(context, "Could not launch ${app.appName}", Toast.LENGTH_SHORT).show()
                      }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isGoLoginApp) CyanPrimary else GreenSuccess
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("play_app_${app.packageName}")
                  ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Permission Explainer Dialog for Unknown App Sources
  if (showInstallPermissionDialog) {
    AlertDialog(
      onDismissRequest = { showInstallPermissionDialog = false },
      icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
      title = { Text("Allow APK Installation") },
      text = {
        Text("To install GoLogin software and APK packages on this phone, Android requires permission to 'Install unknown apps'. Tap 'Open Settings' and switch the toggle to ON, then return here to install.")
      },
      confirmButton = {
        Button(
          onClick = {
            showInstallPermissionDialog = false
            ApkManager.openInstallPermissionSettings(context)
          }
        ) {
          Text("Open Settings")
        }
      },
      dismissButton = {
        TextButton(onClick = { showInstallPermissionDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Direct URL Downloader Dialog
  if (showDownloadDialog) {
    AlertDialog(
      onDismissRequest = { if (!isDownloading) showDownloadDialog = false },
      title = { Text("Download APK from URL") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Enter direct link to an Android APK (such as GoLogin mobile release or custom browser build):",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = downloadUrlInput,
            onValueChange = { downloadUrlInput = it },
            placeholder = { Text("https://example.com/app.apk") },
            enabled = !isDownloading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          if (isDownloading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { downloadProgress / 100f },
              modifier = Modifier.fillMaxWidth()
            )
            Text(
              text = "Downloading APK: $downloadProgress%",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      },
      confirmButton = {
        Button(
          enabled = downloadUrlInput.isNotBlank() && !isDownloading,
          onClick = {
            isDownloading = true
            downloadProgress = 0
            val fileName = downloadUrlInput.substringAfterLast("/").takeIf { it.isNotBlank() && it.endsWith(".apk") }
              ?: "download_${System.currentTimeMillis()}.apk"

            coroutineScope.launch {
              val result = ApkManager.downloadApk(
                context = context,
                url = downloadUrlInput.trim(),
                suggestedFileName = fileName,
                onProgress = { p -> downloadProgress = p }
              )
              isDownloading = false
              if (result.isSuccess) {
                showDownloadDialog = false
                val downloadedFile = result.getOrNull()
                if (downloadedFile != null) {
                  val apk = ApkManager.extractApkInfo(context, downloadedFile)
                  selectedApk = apk
                  Toast.makeText(context, "Download complete: ${downloadedFile.name}", Toast.LENGTH_SHORT).show()
                  reloadData()
                }
              } else {
                Toast.makeText(
                  context,
                  "Download failed: ${result.exceptionOrNull()?.message}",
                  Toast.LENGTH_LONG
                ).show()
              }
            }
          }
        ) {
          Text(if (isDownloading) "Downloading..." else "Download & Inspect")
        }
      },
      dismissButton = {
        if (!isDownloading) {
          TextButton(onClick = { showDownloadDialog = false }) {
            Text("Cancel")
          }
        }
      }
    )
  }
}

// Extension to format Double nicely
private fun Double.format(digits: Int) = java.lang.String.format(java.util.Locale.US, "%.${digits}f", this)
