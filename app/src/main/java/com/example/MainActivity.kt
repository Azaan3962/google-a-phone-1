package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppHeader
import com.example.ui.screens.ApkInstallerScreen
import com.example.ui.screens.GoLoginWebScreen
import com.example.ui.screens.PhoneDiagnosticScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ApkManager
import com.example.util.ProfileStorage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val initialIncomingApkUri: Uri? = if (intent?.action == Intent.ACTION_VIEW && intent?.data != null) {
      intent.data
    } else {
      null
    }

    setContent {
      MyApplicationTheme {
        MainAppContent(initialIncomingApkUri = initialIncomingApkUri)
      }
    }
  }
}

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val testTag: String) {
  APK_INSTALLER("APK Hub", Icons.Default.InstallMobile, "nav_apk_hub"),
  GOLOGIN_WEB("GoLogin Web", Icons.Default.Language, "nav_gologin_web"),
  PROFILES("Phone Nodes", Icons.Default.Devices, "nav_profiles"),
  DIAGNOSTICS("Phone Guide", Icons.Default.Fingerprint, "nav_diagnostics")
}

@Composable
fun MainAppContent(initialIncomingApkUri: Uri? = null) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedTab by remember { mutableStateOf(AppTab.APK_INSTALLER) }
  var activeProfileId by remember { mutableStateOf(ProfileStorage.getActiveProfileId(context)) }
  val profiles = remember { ProfileStorage.getProfiles(context) }
  val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()

  // Handle incoming APK file if opened via file manager
  LaunchedEffect(initialIncomingApkUri) {
    if (initialIncomingApkUri != null) {
      coroutineScope.launch {
        try {
          val saved = ApkManager.saveApkFromUri(context, initialIncomingApkUri)
          if (saved != null) {
            Toast.makeText(context, "Loaded incoming APK: ${saved.name}", Toast.LENGTH_SHORT).show()
            selectedTab = AppTab.APK_INSTALLER
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  Scaffold(
    topBar = {
      val title = when (selectedTab) {
        AppTab.APK_INSTALLER -> "APK Runner & Installer"
        AppTab.GOLOGIN_WEB -> "GoLogin Web App"
        AppTab.PROFILES -> "Multi-Phone Profiles"
        AppTab.DIAGNOSTICS -> "Device & GoLogin Setup"
      }
      val subtitle = when (selectedTab) {
        AppTab.APK_INSTALLER -> "Upload, inspect, install, and play APK packages"
        AppTab.GOLOGIN_WEB -> "Official cloud app with isolated sessions & desktop mode"
        AppTab.PROFILES -> "Manage independent GoLogin profiles per phone"
        AppTab.DIAGNOSTICS -> "Hardware fingerprint and multi-device setup guide"
      }

      AppHeader(
        title = title,
        subtitle = subtitle,
        activePhoneTag = activeProfile?.name ?: "Phone Node",
        onTagClick = {
          selectedTab = AppTab.PROFILES
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
      ) {
        AppTab.values().forEach { tab ->
          val selected = selectedTab == tab
          NavigationBarItem(
            selected = selected,
            onClick = { selectedTab = tab },
            icon = {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = if (selected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            label = {
              Text(
                text = tab.title,
                color = if (selected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            modifier = Modifier.testTag(tab.testTag)
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
      when (selectedTab) {
        AppTab.APK_INSTALLER -> {
          ApkInstallerScreen(
            onNavigateToGoLoginWeb = { selectedTab = AppTab.GOLOGIN_WEB }
          )
        }
        AppTab.GOLOGIN_WEB -> {
          GoLoginWebScreen(
            initialProfileId = activeProfileId
          )
        }
        AppTab.PROFILES -> {
          ProfilesScreen(
            onOpenProfileInWeb = { profileId ->
              activeProfileId = profileId
              selectedTab = AppTab.GOLOGIN_WEB
            }
          )
        }
        AppTab.DIAGNOSTICS -> {
          PhoneDiagnosticScreen(
            onNavigateToApkInstaller = { selectedTab = AppTab.APK_INSTALLER },
            onNavigateToGoLoginWeb = { selectedTab = AppTab.GOLOGIN_WEB }
          )
        }
      }
    }
  }
}
