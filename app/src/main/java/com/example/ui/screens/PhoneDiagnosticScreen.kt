package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceDiagnostic
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GreenSuccess
import com.example.util.ApkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone

@Composable
fun PhoneDiagnosticScreen(
  onNavigateToApkInstaller: () -> Unit,
  onNavigateToGoLoginWeb: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var diagnostic by remember {
    mutableStateOf(
      DeviceDiagnostic(
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        androidVersion = Build.VERSION.RELEASE,
        apiLevel = Build.VERSION.SDK_INT,
        displayMetrics = "${context.resources.displayMetrics.widthPixels} x ${context.resources.displayMetrics.heightPixels} (${context.resources.displayMetrics.densityDpi} dpi)",
        language = Locale.getDefault().toLanguageTag(),
        timeZone = TimeZone.getDefault().id,
        publicIp = "Checking public IP...",
        isGoLoginInstalled = false,
        goLoginPackageName = null
      )
    )
  }

  var isCheckingIp by remember { mutableStateOf(false) }

  fun checkOfficialAppAndIp() {
    coroutineScope.launch {
      // Check GoLogin package
      val goLoginCandidates = listOf("com.gologin", "com.gologin.android", "gologin.browser", "com.gologin.mobile")
      var detectedPkg: String? = null
      for (pkg in goLoginCandidates) {
        val (installed, _, _) = ApkManager.checkInstalledPackage(context.packageManager, pkg)
        if (installed) {
          detectedPkg = pkg
          break
        }
      }

      diagnostic = diagnostic.copy(
        isGoLoginInstalled = detectedPkg != null,
        goLoginPackageName = detectedPkg
      )

      // Fetch IP
      isCheckingIp = true
      val ipData = withContext(Dispatchers.IO) {
        try {
          val client = OkHttpClient()
          val request = Request.Builder().url("https://api.ipify.org?format=json").build()
          val response = client.newCall(request).execute()
          if (response.isSuccessful) {
            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            json.optString("ip", "Unknown")
          } else {
            "Offline / Private Node"
          }
        } catch (e: Exception) {
          "Offline / Private Node"
        }
      }

      diagnostic = diagnostic.copy(
        publicIp = ipData
      )
      isCheckingIp = false
    }
  }

  LaunchedEffect(Unit) {
    checkOfficialAppAndIp()
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
  ) {
    // Official GoLogin Software Status on THIS Phone
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (diagnostic.isGoLoginInstalled) Color(0xFF064E3B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (diagnostic.isGoLoginInstalled) GreenSuccess else MaterialTheme.colorScheme.primary
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
                .size(48.dp)
                .clip(CircleShape)
                .background(
                  if (diagnostic.isGoLoginInstalled) GreenSuccess.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (diagnostic.isGoLoginInstalled) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                tint = if (diagnostic.isGoLoginInstalled) GreenSuccess else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Official GoLogin Software Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (diagnostic.isGoLoginInstalled) "Official client installed on this device" else "Ready to run official GoLogin on this phone",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          if (diagnostic.isGoLoginInstalled) {
            StatusBadge(
              text = "OFFICIAL GOLOGIN APP READY (${diagnostic.goLoginPackageName})",
              isPositive = true,
              icon = Icons.Default.Verified
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = {
                diagnostic.goLoginPackageName?.let { pkg ->
                  ApkManager.launchApp(context, pkg)
                }
              },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("launch_official_gologin_button")
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Launch Official GoLogin App", color = Color.White, fontWeight = FontWeight.Bold)
            }
          } else {
            Text(
              text = "This phone can run official GoLogin in two ways:\n1. Open GoLogin Web App directly inside this app\n2. Download & install the official GoLogin Android APK",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = onNavigateToGoLoginWeb,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Web App", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }

              OutlinedButton(
                onClick = onNavigateToApkInstaller,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get Official APK", fontSize = 12.sp)
              }
            }
          }
        }
      }
    }

    // Hardware Isolation & Fingerprint Parameters
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "Phone Hardware & Network Fingerprint",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Native parameters that distinguish this phone from other devices",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            IconButton(onClick = { checkOfficialAppAndIp() }) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh Diagnostics")
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // IP card
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text("Public Node IP Address", style = MaterialTheme.typography.labelSmall)
                Text(
                  text = diagnostic.publicIp,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                  )
                )
              }
              if (isCheckingIp) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          ParameterRow(label = "Device Model", value = "${diagnostic.manufacturer} ${diagnostic.model}")
          ParameterRow(label = "Android OS", value = "Android ${diagnostic.androidVersion} (API ${diagnostic.apiLevel})")
          ParameterRow(label = "Display Resolution", value = diagnostic.displayMetrics)
          ParameterRow(label = "System Locale", value = diagnostic.language)
          ParameterRow(label = "Timezone", value = diagnostic.timeZone)
          ParameterRow(label = "Supported ABIs", value = Build.SUPPORTED_ABIS.joinToString(", "))
        }
      }
    }

    // Comprehensive Guide: Running GoLogin Separately on Each Phone
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = CyanPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Guide: Separate GoLogin On Each Phone",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          GuideStep(
            number = "1",
            title = "Hardware Isolation (Native Advantage)",
            description = "Each physical phone has its own unique GPU, Canvas rendering pipeline, and hardware serials. When using GoLogin on different phones, anti-fraud algorithms see 100% genuine independent mobile hardware."
          )

          GuideStep(
            number = "2",
            title = "Independent GoLogin Web App Session",
            description = "Use the 'GoLogin Web' tab in this app on each phone. Each device keeps its own isolated cookie jar, local storage, and profile node without cross-contamination."
          )

          GuideStep(
            number = "3",
            title = "Separate Network / Mobile IP per Phone",
            description = "For maximum separation, give each phone its own cellular 4G/5G SIM card or dedicated proxy. This ensures each phone has a distinct residential IP address."
          )

          GuideStep(
            number = "4",
            title = "Upload & Install Any Official APK",
            description = "Use the 'APK Installer' tab to upload or download the official GoLogin APK, Orbita browser, or companion apps directly to the phone's internal storage."
          )
        }
      }
    }
  }
}

@Composable
private fun ParameterRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun GuideStep(number: String, title: String, description: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(CyanPrimary),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = number,
        color = Color.Black,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
