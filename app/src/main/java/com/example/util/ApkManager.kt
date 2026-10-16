package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.model.ApkInfo
import com.example.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object ApkManager {

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

  fun getApksDirectory(context: Context): File {
    val dir = File(context.filesDir, "apks")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  suspend fun saveApkFromUri(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
    try {
      var displayName = "app_${System.currentTimeMillis()}.apk"
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
          val name = cursor.getString(nameIndex)
          if (!name.isNullOrBlank()) {
            displayName = if (name.endsWith(".apk", ignoreCase = true)) name else "$name.apk"
          }
        }
      }

      val targetFile = File(getApksDirectory(context), displayName)
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(targetFile).use { output ->
          input.copyTo(output)
        }
      }
      targetFile
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun extractApkInfo(context: Context, file: File): ApkInfo? {
    if (!file.exists()) return null
    return try {
      val pm = context.packageManager
      val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES
      val packageInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
      } else {
        @Suppress("DEPRECATION")
        pm.getPackageArchiveInfo(file.absolutePath, flags)
      }

      if (packageInfo == null) return null

      // Required for PackageManager to resolve resource labels and icons from archive
      packageInfo.applicationInfo?.sourceDir = file.absolutePath
      packageInfo.applicationInfo?.publicSourceDir = file.absolutePath

      val appName = try {
        packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: file.nameWithoutExtension
      } catch (e: Exception) {
        file.nameWithoutExtension
      }

      val iconBitmap: Bitmap? = try {
        packageInfo.applicationInfo?.loadIcon(pm)?.let { drawableToBitmap(it) }
      } catch (e: Exception) {
        null
      }

      val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        packageInfo.applicationInfo?.minSdkVersion ?: 21
      } else {
        21
      }

      val targetSdk = packageInfo.applicationInfo?.targetSdkVersion ?: 21

      val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

      val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
      } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
      }

      // Check if installed on current phone
      val (isInstalled, installedVerName, installedVerCode) = checkInstalledPackage(pm, packageInfo.packageName)

      ApkInfo(
        id = file.absolutePath.hashCode().toString(),
        fileName = file.name,
        filePath = file.absolutePath,
        appName = appName,
        packageName = packageInfo.packageName,
        versionName = packageInfo.versionName ?: "1.0",
        versionCode = versionCode,
        minSdk = minSdk,
        targetSdk = targetSdk,
        fileSizeBytes = file.length(),
        permissions = permissions,
        icon = iconBitmap,
        isInstalled = isInstalled,
        installedVersionName = installedVerName,
        installedVersionCode = installedVerCode
      )
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun checkInstalledPackage(pm: PackageManager, packageName: String): Triple<Boolean, String?, Long?> {
    return try {
      val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(packageName, 0)
      }
      val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
      } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
      }
      Triple(true, info.versionName, vCode)
    } catch (e: PackageManager.NameNotFoundException) {
      Triple(false, null, null)
    } catch (e: Exception) {
      Triple(false, null, null)
    }
  }

  fun canRequestPackageInstalls(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.packageManager.canRequestPackageInstalls()
    } else {
      true
    }
  }

  fun openInstallPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}")
      ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      try {
        context.startActivity(intent)
      } catch (e: Exception) {
        // Fallback to general security settings
        val fallback = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
      }
    }
  }

  fun installApk(context: Context, file: File): Result<Unit> {
    return try {
      if (!file.exists()) {
        return Result.failure(IllegalStateException("APK file does not exist: ${file.name}"))
      }

      val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun launchApp(context: Context, packageName: String): Boolean {
    return try {
      val intent = context.packageManager.getLaunchIntentForPackage(packageName)
      if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
      } else {
        false
      }
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  fun getInstalledApps(context: Context, onlyUserApps: Boolean = false): List<InstalledAppInfo> {
    val pm = context.packageManager
    return try {
      val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(0)
      }

      packages.mapNotNull { pkg ->
        val appInfo = pkg.applicationInfo ?: return@mapNotNull null
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        if (onlyUserApps && isSystem) return@mapNotNull null

        // Check if playable/launchable
        val hasLaunchIntent = pm.getLaunchIntentForPackage(pkg.packageName) != null
        if (!hasLaunchIntent) return@mapNotNull null

        val appName = try {
          appInfo.loadLabel(pm).toString()
        } catch (e: Exception) {
          pkg.packageName
        }

        val icon = try {
          appInfo.loadIcon(pm)
        } catch (e: Exception) {
          null
        }

        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          pkg.longVersionCode
        } else {
          @Suppress("DEPRECATION")
          pkg.versionCode.toLong()
        }

        InstalledAppInfo(
          appName = appName,
          packageName = pkg.packageName,
          versionName = pkg.versionName ?: "1.0",
          versionCode = vCode,
          icon = icon,
          isSystemApp = isSystem,
          installTime = pkg.firstInstallTime
        )
      }.sortedWith(compareByDescending<InstalledAppInfo> {
        it.packageName.contains("gologin", ignoreCase = true)
      }.thenBy { it.appName.lowercase() })
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  suspend fun downloadApk(
    context: Context,
    url: String,
    suggestedFileName: String,
    onProgress: (Int) -> Unit
  ): Result<File> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder().url(url).build()
      val response = okHttpClient.newCall(request).execute()

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("Download failed with HTTP ${response.code}"))
      }

      val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
      val totalLength = body.contentLength()

      val cleanFileName = if (suggestedFileName.endsWith(".apk", ignoreCase = true)) {
        suggestedFileName
      } else {
        "$suggestedFileName.apk"
      }

      val targetFile = File(getApksDirectory(context), cleanFileName)
      body.byteStream().use { input ->
        FileOutputStream(targetFile).use { output ->
          val buffer = ByteArray(8192)
          var bytesCopied = 0L
          var read: Int
          while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            bytesCopied += read
            if (totalLength > 0) {
              val progress = ((bytesCopied * 100) / totalLength).toInt()
              withContext(Dispatchers.Main) {
                onProgress(progress)
              }
            }
          }
        }
      }
      withContext(Dispatchers.Main) { onProgress(100) }
      Result.success(targetFile)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  fun getAllStoredApks(context: Context): List<ApkInfo> {
    val dir = getApksDirectory(context)
    val files = dir.listFiles { f -> f.extension.equals("apk", ignoreCase = true) } ?: emptyArray()
    return files.mapNotNull { file ->
      extractApkInfo(context, file)
    }.sortedByDescending { it.addedAt }
  }

  fun deleteApk(file: File): Boolean {
    return try {
      if (file.exists()) file.delete() else false
    } catch (e: Exception) {
      false
    }
  }

  private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
      return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }
}
