package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GoLoginProfile
import org.json.JSONArray
import org.json.JSONObject

object ProfileStorage {
  private const val PREF_NAME = "gologin_profile_prefs"
  private const val KEY_PROFILES = "saved_profiles"
  private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
  private const val KEY_DESKTOP_MODE = "is_desktop_mode"

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
  }

  fun getProfiles(context: Context): List<GoLoginProfile> {
    val jsonString = getPrefs(context).getString(KEY_PROFILES, null)
    if (jsonString.isNullOrBlank()) {
      // Initialize with default phone profiles for quick start
      val defaults = listOf(
        GoLoginProfile(
          id = "profile_phone_1",
          name = "Phone #1 - Primary Node",
          deviceTag = "Primary Device",
          notes = "Isolated official GoLogin web session for Phone 1",
          userAgentMode = "Desktop Chrome",
          proxyType = "Direct (No Proxy)"
        ),
        GoLoginProfile(
          id = "profile_phone_2",
          name = "Phone #2 - Cloud Worker",
          deviceTag = "Secondary Device",
          notes = "Separate session with custom user-agent & isolated cookie store",
          userAgentMode = "Desktop Chrome",
          proxyType = "Direct (No Proxy)"
        )
      )
      saveProfiles(context, defaults)
      return defaults
    }

    return try {
      val jsonArray = JSONArray(jsonString)
      val list = mutableListOf<GoLoginProfile>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
          GoLoginProfile(
            id = obj.optString("id", "profile_$i"),
            name = obj.optString("name", "Profile $i"),
            deviceTag = obj.optString("deviceTag", "Device"),
            notes = obj.optString("notes", ""),
            userAgentMode = obj.optString("userAgentMode", "Desktop Chrome"),
            customUserAgent = obj.optString("customUserAgent", ""),
            proxyType = obj.optString("proxyType", "Direct (No Proxy)"),
            proxyHost = obj.optString("proxyHost", ""),
            proxyPort = obj.optString("proxyPort", ""),
            proxyUser = obj.optString("proxyUser", ""),
            proxyPass = obj.optString("proxyPass", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }
      list
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  fun saveProfiles(context: Context, profiles: List<GoLoginProfile>) {
    val array = JSONArray()
    for (p in profiles) {
      val obj = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("deviceTag", p.deviceTag)
        put("notes", p.notes)
        put("userAgentMode", p.userAgentMode)
        put("customUserAgent", p.customUserAgent)
        put("proxyType", p.proxyType)
        put("proxyHost", p.proxyHost)
        put("proxyPort", p.proxyPort)
        put("proxyUser", p.proxyUser)
        put("proxyPass", p.proxyPass)
        put("createdAt", p.createdAt)
      }
      array.put(obj)
    }
    getPrefs(context).edit().putString(KEY_PROFILES, array.toString()).apply()
  }

  fun getActiveProfileId(context: Context): String {
    return getPrefs(context).getString(KEY_ACTIVE_PROFILE_ID, "profile_phone_1") ?: "profile_phone_1"
  }

  fun setActiveProfileId(context: Context, id: String) {
    getPrefs(context).edit().putString(KEY_ACTIVE_PROFILE_ID, id).apply()
  }

  fun isDesktopModeEnabled(context: Context): Boolean {
    return getPrefs(context).getBoolean(KEY_DESKTOP_MODE, true)
  }

  fun setDesktopModeEnabled(context: Context, enabled: Boolean) {
    getPrefs(context).edit().putBoolean(KEY_DESKTOP_MODE, enabled).apply()
  }
}
