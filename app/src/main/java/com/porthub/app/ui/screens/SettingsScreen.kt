package com.porthub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porthub.app.ui.components.GlassButton
import com.porthub.app.ui.components.glass
import com.porthub.app.ui.theme.AppText
import com.porthub.app.ui.theme.Ink
import com.porthub.app.ui.theme.LocalAppLayout

@Composable
fun SettingsScreen(
    feedUrl: String,
    source: String?,
    gameCount: Int,
    refreshing: Boolean,
    onApplyFeed: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val layout = LocalAppLayout.current
    val context = LocalContext.current
    var draft by remember(feedUrl) { mutableStateOf(feedUrl) }

    fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Settings",
                style = AppText.LargeTitle,
                modifier = Modifier.padding(start = layout.gutter, end = layout.gutter, top = 8.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$gameCount ports · ${layout.widthDp}dp ${if (layout.isWide) "tablet" else "phone"} layout",
                style = AppText.Caption.copy(fontSize = 12.sp),
                color = Ink.TextTertiary,
                modifier = Modifier.padding(horizontal = layout.gutter),
            )

            Spacer(Modifier.height(22.dp))

            SectionLabel("CATALOG FEED", layout.gutter)
            GlassCard {
                Text("Games are fetched from this JSON file.", style = AppText.Caption.copy(fontSize = 12.sp), color = Ink.TextTertiary)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.28f))
                        .border(0.7.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = AppText.Caption.copy(fontSize = 12.sp, color = Ink.TextPrimary),
                        cursorBrush = SolidColor(Ink.Accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("Apply", onClick = { onApplyFeed(draft) }, primary = true)
                    GlassButton(if (refreshing) "Refreshing…" else "Refresh", onClick = onRefresh)
                }
                Spacer(Modifier.height(14.dp))
                InfoLine("Ports loaded", gameCount.toString())
                InfoLine(
                    "Source",
                    when (source) {
                        "NETWORK" -> "Network"
                        "CACHE" -> "Disk cache"
                        "BUNDLED" -> "Bundled copy"
                        else -> "—"
                    },
                )
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("APPEARANCE", layout.gutter)
            GlassCard {
                InfoLine("Layout", if (layout.isWide) "Tablet — side rail" else "Phone — bottom bar")
                InfoLine("Grid", "${layout.columns} columns")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Layout follows the window automatically: rotate the device or open it on a tablet and it adapts.",
                    style = AppText.Caption.copy(fontSize = 12.sp),
                    color = Ink.TextTertiary,
                )
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("PROJECT", layout.gutter)
            GlassCard {
                LinkLine("Source code") { open("https://github.com/orev7s/porthub") }
                LinkLine("Catalog repo") { open("https://github.com/orev7s/porthub-catalog") }
                Spacer(Modifier.height(6.dp))
                InfoLine("Version", "0.5.0")
                InfoLine("License", "GPL-3.0")
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("LEGAL", layout.gutter)
            GlassCard {
                Text(
                    "PortHub links to ports; it never hosts game data. Many builds need files from a copy of the " +
                        "original game you already own — every game page says exactly which ones.",
                    style = AppText.Body.copy(fontSize = 13.sp),
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String, gutter: androidx.compose.ui.unit.Dp) {
    Text(
        text,
        style = AppText.Eyebrow,
        modifier = Modifier.padding(start = gutter, end = gutter, bottom = 8.dp),
    )
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    val layout = LocalAppLayout.current
    Column(
        Modifier
            .padding(horizontal = layout.gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .glass(shape = RoundedCornerShape(20.dp), shadowElevation = 0.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = AppText.Caption.copy(fontSize = 13.sp), color = Ink.TextSecondary)
        Spacer(Modifier.size(12.dp))
        Text(
            value,
            style = AppText.Caption.copy(fontSize = 13.sp),
            color = Ink.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LinkLine(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = AppText.Caption.copy(fontSize = 13.sp), color = Ink.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Open", style = AppText.Caption.copy(fontSize = 12.sp), color = Ink.Accent)
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = Ink.TextTertiary, modifier = Modifier.size(16.dp))
        }
    }
}

