package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoLoginProfile
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GreenSuccess
import com.example.util.ProfileStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
  onOpenProfileInWeb: (String) -> Unit
) {
  val context = LocalContext.current

  var profiles by remember { mutableStateOf(ProfileStorage.getProfiles(context)) }
  var activeProfileId by remember { mutableStateOf(ProfileStorage.getActiveProfileId(context)) }

  var showAddDialog by remember { mutableStateOf(false) }
  var editingProfile by remember { mutableStateOf<GoLoginProfile?>(null) }

  // Form states
  var formName by remember { mutableStateOf("") }
  var formDeviceTag by remember { mutableStateOf("") }
  var formNotes by remember { mutableStateOf("") }
  var formUserAgentMode by remember { mutableStateOf("Desktop Chrome") }
  var formCustomUserAgent by remember { mutableStateOf("") }
  var formProxyType by remember { mutableStateOf("Direct (No Proxy)") }
  var formProxyHost by remember { mutableStateOf("") }
  var formProxyPort by remember { mutableStateOf("") }

  fun openCreateDialog() {
    formName = "Phone #${profiles.size + 1} Profile"
    formDeviceTag = "Phone ${profiles.size + 1}"
    formNotes = "Independent browser profile & session for Phone ${profiles.size + 1}"
    formUserAgentMode = "Desktop Chrome"
    formCustomUserAgent = ""
    formProxyType = "Direct (No Proxy)"
    formProxyHost = ""
    formProxyPort = ""
    editingProfile = null
    showAddDialog = true
  }

  fun openEditDialog(profile: GoLoginProfile) {
    editingProfile = profile
    formName = profile.name
    formDeviceTag = profile.deviceTag
    formNotes = profile.notes
    formUserAgentMode = profile.userAgentMode
    formCustomUserAgent = profile.customUserAgent
    formProxyType = profile.proxyType
    formProxyHost = profile.proxyHost
    formProxyPort = profile.proxyPort
    showAddDialog = true
  }

  fun saveForm() {
    if (formName.isBlank()) {
      Toast.makeText(context, "Please enter a profile name", Toast.LENGTH_SHORT).show()
      return
    }

    val updatedList = profiles.toMutableList()
    if (editingProfile != null) {
      val index = updatedList.indexOfFirst { it.id == editingProfile!!.id }
      if (index != -1) {
        updatedList[index] = editingProfile!!.copy(
          name = formName.trim(),
          deviceTag = formDeviceTag.trim(),
          notes = formNotes.trim(),
          userAgentMode = formUserAgentMode,
          customUserAgent = formCustomUserAgent.trim(),
          proxyType = formProxyType,
          proxyHost = formProxyHost.trim(),
          proxyPort = formProxyPort.trim()
        )
      }
    } else {
      val newId = "profile_${System.currentTimeMillis()}"
      updatedList.add(
        GoLoginProfile(
          id = newId,
          name = formName.trim(),
          deviceTag = formDeviceTag.trim().ifBlank { "Phone ${updatedList.size + 1}" },
          notes = formNotes.trim(),
          userAgentMode = formUserAgentMode,
          customUserAgent = formCustomUserAgent.trim(),
          proxyType = formProxyType,
          proxyHost = formProxyHost.trim(),
          proxyPort = formProxyPort.trim()
        )
      )
    }

    profiles = updatedList
    ProfileStorage.saveProfiles(context, updatedList)
    showAddDialog = false
    Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { openCreateDialog() },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("add_profile_fab")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Phone Profile")
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
      // Guide Card
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
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Devices,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Multi-Phone Isolation Hub",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Configure separate profiles so each phone runs its own isolated GoLogin software instance",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Active Profiles (${profiles.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          TextButton(onClick = { openCreateDialog() }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Phone Profile")
          }
        }
      }

      items(profiles) { profile ->
        val isActive = profile.id == activeProfileId
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
          ),
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
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) GreenSuccess else Color.Gray)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = profile.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
              }

              if (isActive) {
                StatusBadge(text = "CURRENT ACTIVE NODE", isPositive = true)
              } else {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Text(
                    text = profile.deviceTag,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }

            if (profile.notes.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = profile.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              SuggestionChip(
                onClick = {},
                label = { Text("UA: ${profile.userAgentMode}", fontSize = 11.sp) }
              )
              SuggestionChip(
                onClick = {},
                label = { Text("Proxy: ${profile.proxyType}", fontSize = 11.sp) }
              )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  activeProfileId = profile.id
                  ProfileStorage.setActiveProfileId(context, profile.id)
                  onOpenProfileInWeb(profile.id)
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isActive) CyanPrimary else MaterialTheme.colorScheme.primaryContainer,
                  contentColor = if (isActive) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(42.dp)
                  .testTag("launch_profile_${profile.id}")
              ) {
                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isActive) "Open in GoLogin Web" else "Activate & Open Web", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }

              IconButton(
                onClick = { openEditDialog(profile) },
                modifier = Modifier.size(42.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
              }

              if (profiles.size > 1) {
                IconButton(
                  onClick = {
                    val remaining = profiles.filterNot { it.id == profile.id }
                    profiles = remaining
                    ProfileStorage.saveProfiles(context, remaining)
                    if (activeProfileId == profile.id) {
                      activeProfileId = remaining.first().id
                      ProfileStorage.setActiveProfileId(context, activeProfileId)
                    }
                    Toast.makeText(context, "Profile deleted", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(42.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
    }
  }

  // Add / Edit Profile Dialog
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text(if (editingProfile != null) "Edit Phone Profile" else "Create New Phone Profile") },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = formName,
            onValueChange = { formName = it },
            label = { Text("Profile Name") },
            placeholder = { Text("e.g. Phone #2 - Worker") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = formDeviceTag,
            onValueChange = { formDeviceTag = it },
            label = { Text("Device Identifier / Tag") },
            placeholder = { Text("e.g. Galaxy S24, Pixel 8") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = formNotes,
            onValueChange = { formNotes = it },
            label = { Text("Notes & Purpose") },
            placeholder = { Text("Separate accounts / geo routing") },
            modifier = Modifier.fillMaxWidth()
          )

          Text("Browser Mode & User Agent:", style = MaterialTheme.typography.labelMedium)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = formUserAgentMode == "Desktop Chrome",
              onClick = { formUserAgentMode = "Desktop Chrome" },
              label = { Text("Desktop", fontSize = 11.sp) }
            )
            FilterChip(
              selected = formUserAgentMode == "Android Mobile",
              onClick = { formUserAgentMode = "Android Mobile" },
              label = { Text("Mobile", fontSize = 11.sp) }
            )
            FilterChip(
              selected = formUserAgentMode == "Custom",
              onClick = { formUserAgentMode = "Custom" },
              label = { Text("Custom", fontSize = 11.sp) }
            )
          }

          if (formUserAgentMode == "Custom") {
            OutlinedTextField(
              value = formCustomUserAgent,
              onValueChange = { formCustomUserAgent = it },
              label = { Text("Custom User-Agent String") },
              modifier = Modifier.fillMaxWidth()
            )
          }

          Text("Proxy Setup (Optional):", style = MaterialTheme.typography.labelMedium)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = formProxyType == "Direct (No Proxy)",
              onClick = { formProxyType = "Direct (No Proxy)" },
              label = { Text("Direct", fontSize = 11.sp) }
            )
            FilterChip(
              selected = formProxyType == "HTTP",
              onClick = { formProxyType = "HTTP" },
              label = { Text("HTTP", fontSize = 11.sp) }
            )
            FilterChip(
              selected = formProxyType == "SOCKS5",
              onClick = { formProxyType = "SOCKS5" },
              label = { Text("SOCKS5", fontSize = 11.sp) }
            )
          }

          if (formProxyType != "Direct (No Proxy)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = formProxyHost,
                onValueChange = { formProxyHost = it },
                label = { Text("Host / IP") },
                modifier = Modifier.weight(2f),
                singleLine = true
              )
              OutlinedTextField(
                value = formProxyPort,
                onValueChange = { formProxyPort = it },
                label = { Text("Port") },
                modifier = Modifier.weight(1f),
                singleLine = true
              )
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { saveForm() }) {
          Text("Save Profile")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
