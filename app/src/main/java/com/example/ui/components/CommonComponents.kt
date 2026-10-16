package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GreenSuccess

@Composable
fun AppHeader(
  title: String,
  subtitle: String? = null,
  activePhoneTag: String? = null,
  onTagClick: (() -> Unit)? = null
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("screen_header_title")
          )
          if (!subtitle.isNullOrBlank()) {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (!activePhoneTag.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .clickable(enabled = onTagClick != null) { onTagClick?.invoke() }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(GreenSuccess)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = activePhoneTag,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun StatusBadge(
  text: String,
  isPositive: Boolean,
  icon: ImageVector? = null,
  modifier: Modifier = Modifier
) {
  val bgColor = if (isPositive) Color(0xFF064E3B) else Color(0xFF451A03)
  val textColor = if (isPositive) Color(0xFF6EE7B7) else Color(0xFFFDBA74)

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(bgColor)
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = textColor,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
    }
    Text(
      text = text,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
