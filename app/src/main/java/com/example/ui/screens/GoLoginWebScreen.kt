package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.GoLoginProfile
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GreenSuccess
import com.example.util.ProfileStorage

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoLoginWebScreen(
  initialProfileId: String? = null
) {
  val context = LocalContext.current

  var webView by remember { mutableStateOf<WebView?>(null) }
  var currentUrl by remember { mutableStateOf("https://app.gologin.com/") }
  var pageTitle by remember { mutableStateOf("GoLogin Cloud Web App") }
  var loadProgress by remember { mutableStateOf(0) }
  var isLoading by remember { mutableStateOf(true) }
  var canGoBack by remember { mutableStateOf(false) }
  var canGoForward by remember { mutableStateOf(false) }

  // Preferences & Profiles
  var profiles by remember { mutableStateOf(ProfileStorage.getProfiles(context)) }
  var activeProfileId by remember {
    mutableStateOf(initialProfileId ?: ProfileStorage.getActiveProfileId(context))
  }
  var isDesktopMode by remember { mutableStateOf(ProfileStorage.isDesktopModeEnabled(context)) }
  var showProfileMenu by remember { mutableStateOf(false) }
  var showQuickLinksMenu by remember { mutableStateOf(false) }

  val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()

  val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
  val mobileUserAgent = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

  // Handle hardware back button in WebView
  BackHandler(enabled = canGoBack) {
    webView?.goBack()
  }

  fun applyUserAgent(wv: WebView, desktop: Boolean) {
    val ua = when {
      activeProfile?.customUserAgent?.isNotBlank() == true -> activeProfile.customUserAgent
      desktop -> desktopUserAgent
      else -> mobileUserAgent
    }
    wv.settings.userAgentString = ua
    wv.settings.useWideViewPort = desktop
    wv.settings.loadWithOverviewMode = desktop
  }

  Scaffold(
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Top row: Active Profile and Desktop toggle
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Profile selector button
            Box {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { showProfileMenu = true }
                  .testTag("gologin_profile_picker")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(GreenSuccess)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = activeProfile?.name ?: "Default Phone Node",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }

              DropdownMenu(
                expanded = showProfileMenu,
                onDismissRequest = { showProfileMenu = false }
              ) {
                Text(
                  text = "Switch Phone Isolation Node:",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
                profiles.forEach { profile ->
                  DropdownMenuItem(
                    text = {
                      Column {
                        Text(profile.name, fontWeight = FontWeight.Bold)
                        Text(profile.deviceTag, style = MaterialTheme.typography.labelSmall)
                      }
                    },
                    leadingIcon = {
                      Icon(
                        imageVector = if (profile.id == activeProfileId) Icons.Default.CheckCircle else Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = if (profile.id == activeProfileId) GreenSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    },
                    onClick = {
                      showProfileMenu = false
                      activeProfileId = profile.id
                      ProfileStorage.setActiveProfileId(context, profile.id)
                      webView?.let { wv ->
                        applyUserAgent(wv, isDesktopMode)
                        wv.reload()
                      }
                      Toast.makeText(context, "Switched to: ${profile.name}", Toast.LENGTH_SHORT).show()
                    }
                  )
                }
              }
            }

            // Right actions: Desktop View Toggle & Quick Links
            Row(verticalAlignment = Alignment.CenterVertically) {
              FilterChip(
                selected = isDesktopMode,
                onClick = {
                  isDesktopMode = !isDesktopMode
                  ProfileStorage.setDesktopModeEnabled(context, isDesktopMode)
                  webView?.let { wv ->
                    applyUserAgent(wv, isDesktopMode)
                    wv.reload()
                  }
                  Toast.makeText(
                    context,
                    if (isDesktopMode) "Switched to Desktop View (GoLogin Web format)" else "Switched to Mobile View",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                label = {
                  Text(
                    if (isDesktopMode) "Desktop Mode" else "Mobile Mode",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                  )
                },
                modifier = Modifier.testTag("desktop_mode_toggle")
              )

              Spacer(modifier = Modifier.width(4.dp))

              Box {
                IconButton(onClick = { showQuickLinksMenu = true }) {
                  Icon(Icons.Default.MoreVert, contentDescription = "Quick Links")
                }

                DropdownMenu(
                  expanded = showQuickLinksMenu,
                  onDismissRequest = { showQuickLinksMenu = false }
                ) {
                  DropdownMenuItem(
                    text = { Text("GoLogin Web Dashboard") },
                    leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    onClick = {
                      showQuickLinksMenu = false
                      webView?.loadUrl("https://app.gologin.com/")
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("GoLogin Login Page") },
                    leadingIcon = { Icon(Icons.Default.Login, contentDescription = null) },
                    onClick = {
                      showQuickLinksMenu = false
                      webView?.loadUrl("https://app.gologin.com/#/login")
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("Test IP & Browser Leaks (Whoer)") },
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                    onClick = {
                      showQuickLinksMenu = false
                      webView?.loadUrl("https://whoer.net/")
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("Check Fingerprint (BrowserLeaks)") },
                    leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                    onClick = {
                      showQuickLinksMenu = false
                      webView?.loadUrl("https://browserleaks.com/ip")
                    }
                  )
                  HorizontalDivider()
                  DropdownMenuItem(
                    text = { Text("Clear Cookies & Session Cache") },
                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                      showQuickLinksMenu = false
                      CookieManager.getInstance().removeAllCookies(null)
                      CookieManager.getInstance().flush()
                      webView?.clearCache(true)
                      webView?.reload()
                      Toast.makeText(context, "Session cache & cookies cleared for privacy", Toast.LENGTH_SHORT).show()
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("Open in External Browser") },
                    leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
                    onClick = {
                      showQuickLinksMenu = false
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                      context.startActivity(intent)
                    }
                  )
                }
              }
            }
          }

          // Browser Navigation Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { webView?.goBack() },
              enabled = canGoBack,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
            }
            IconButton(
              onClick = { webView?.goForward() },
              enabled = canGoForward,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(20.dp))
            }
            IconButton(
              onClick = { webView?.reload() },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(20.dp))
            }
            IconButton(
              onClick = { webView?.loadUrl("https://app.gologin.com/") },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Current URL display
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .weight(1f)
                .height(34.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (currentUrl.startsWith("https")) Icons.Default.Lock else Icons.Default.Public,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = if (currentUrl.startsWith("https")) GreenSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = currentUrl.removePrefix("https://").removePrefix("http://"),
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // Progress indicator
          if (isLoading) {
            LinearProgressIndicator(
              progress = { loadProgress / 100f },
              color = CyanPrimary,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              databaseEnabled = true
              useWideViewPort = isDesktopMode
              loadWithOverviewMode = isDesktopMode
              setSupportZoom(true)
              builtInZoomControls = true
              displayZoomControls = false
              allowFileAccess = true
              allowContentAccess = true
              cacheMode = WebSettings.LOAD_DEFAULT
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            applyUserAgent(this, isDesktopMode)

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isLoading = true
                url?.let { currentUrl = it }
                canGoBack = view?.canGoBack() == true
                canGoForward = view?.canGoForward() == true
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
                url?.let { currentUrl = it }
                pageTitle = view?.title ?: "GoLogin Web App"
                canGoBack = view?.canGoBack() == true
                canGoForward = view?.canGoForward() == true
              }

              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                  return false // Load in this WebView
                }
                return try {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                  ctx.startActivity(intent)
                  true
                } catch (e: Exception) {
                  false
                }
              }
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                loadProgress = newProgress
                if (newProgress >= 100) {
                  isLoading = false
                }
              }

              override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank()) {
                  pageTitle = title
                }
              }
            }

            loadUrl("https://app.gologin.com/")
            webView = this
          }
        },
        update = { wv ->
          // update settings if desktop mode toggled
          applyUserAgent(wv, isDesktopMode)
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("gologin_webview")
      )
    }
  }
}
