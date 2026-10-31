package com.porthub.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porthub.app.ui.components.GlassButton
import com.porthub.app.ui.components.glass
import com.porthub.app.ui.theme.AppText
import com.porthub.app.ui.theme.Ink

@Composable
fun SettingsScreen(
    feedUrl: String,
    source: String?,
    gameCount: Int,
    refreshing: Boolean,
    onApplyFeed: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var draft by remember(feedUrl) { mutableStateOf(feedUrl) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Settings",
            style = AppText.LargeTitle,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
        )

        Spacer(Modifier.height(22.dp))

        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("CATALOG FEED", style = AppText.Eyebrow)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .glass(shape = RoundedCornerShape(18.dp), shadowElevation = 0.dp)
                    .padding(14.dp),
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = AppText.Caption.copy(fontSize = 13.sp, color = Ink.TextPrimary),
                    cursorBrush = SolidColor(Ink.Accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Apply feed", onClick = { onApplyFeed(draft) }, primary = true)
                GlassButton(if (refreshing) "Refreshing…" else "Refresh", onClick = onRefresh)
            }

            Spacer(Modifier.height(20.dp))
            InfoCard {
                InfoLine("Ports loaded", gameCount.toString())
                InfoLine("Source", when (source) {
                    "NETWORK" -> "Network"
                    "CACHE" -> "Disk cache"
                    "BUNDLED" -> "Bundled seed"
                    else -> "—"
                })
            }

            Spacer(Modifier.height(24.dp))
            Text("ABOUT", style = AppText.Eyebrow)
            Spacer(Modifier.height(8.dp))
            InfoCard {
                Text(
                    "PortHub indexes open-source ports, decompilations and recompilations of games for Android. " +
                        "It never hosts game data — every entry links back to the project that makes it.",
                    style = AppText.Body.copy(fontSize = 14.sp),
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("LEGAL", style = AppText.Eyebrow)
            Spacer(Modifier.height(8.dp))
            InfoCard {
                Text(
                    "Some ports require you to own the original game. PortHub only links to the port's own " +
                        "distribution — no ROMs, ISOs or copyrighted assets are stored in the feed.",
                    style = AppText.Body.copy(fontSize = 14.sp),
                )
            }

            Spacer(Modifier.height(40.dp))
            Text("PortHub 0.1.0 · open source · GPL-3.0", style = AppText.Caption.copy(fontSize = 12.sp), color = Ink.TextTertiary)
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .glass(shape = RoundedCornerShape(20.dp), shadowElevation = 0.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = AppText.Caption.copy(fontSize = 14.sp), color = Ink.TextSecondary)
        Text(value, style = AppText.Caption.copy(fontSize = 14.sp), color = Ink.TextPrimary)
    }
}
