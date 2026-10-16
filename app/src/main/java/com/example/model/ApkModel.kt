package com.example.model

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class ApkInfo(
  val id: String,
  val fileName: String,
  val filePath: String,
  val appName: String,
  val packageName: String,
  val versionName: String,
  val versionCode: Long,
  val minSdk: Int,
  val targetSdk: Int,
  val fileSizeBytes: Long,
  val permissions: List<String> = emptyList(),
  val icon: Bitmap? = null,
  val isInstalled: Boolean = false,
  val installedVersionName: String? = null,
  val installedVersionCode: Long? = null,
  val addedAt: Long = System.currentTimeMillis()
)

data class InstalledAppInfo(
  val appName: String,
  val packageName: String,
  val versionName: String,
  val versionCode: Long,
  val icon: Drawable? = null,
  val isSystemApp: Boolean = false,
  val installTime: Long = 0L
)

data class GoLoginProfile(
  val id: String,
  val name: String,
  val deviceTag: String,
  val notes: String = "",
  val userAgentMode: String = "Desktop Chrome", // Desktop Chrome, Android Mobile, Custom
  val customUserAgent: String = "",
  val proxyType: String = "Direct (No Proxy)", // Direct, HTTP, SOCKS5
  val proxyHost: String = "",
  val proxyPort: String = "",
  val proxyUser: String = "",
  val proxyPass: String = "",
  val createdAt: Long = System.currentTimeMillis()
)

data class DeviceDiagnostic(
  val model: String,
  val manufacturer: String,
  val androidVersion: String,
  val apiLevel: Int,
  val displayMetrics: String,
  val language: String,
  val timeZone: String,
  val publicIp: String = "Checking...",
  val ipCity: String = "",
  val ipCountry: String = "",
  val ipIsp: String = "",
  val isGoLoginInstalled: Boolean = false,
  val goLoginPackageName: String? = null
)

data class DownloadPreset(
  val id: String,
  val title: String,
  val category: String,
  val description: String,
  val directDownloadUrl: String,
  val officialSiteUrl: String,
  val fileName: String,
  val isOfficialGoLogin: Boolean = false
)
