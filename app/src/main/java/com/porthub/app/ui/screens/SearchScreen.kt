package com.porthub.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porthub.app.data.Game
import com.porthub.app.data.PortType
import com.porthub.app.ui.components.GameCard
import com.porthub.app.ui.components.glass
import com.porthub.app.ui.components.pressScale
import com.porthub.app.ui.theme.AppText
import com.porthub.app.ui.theme.Ink

private val filters = listOf(
    "all" to "All",
    PortType.RECOMP to "Recomp",
    PortType.DECOMP to "Decomp",
    PortType.ENGINE to "Engine",
    PortType.SOURCE_PORT to "Source port",
)

@Composable
fun SearchScreen(
    games: List<Game>,
    onOpen: (Game) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    val focusRequester = remember { FocusRequester() }

    val results = remember(games, query, filter) {
        val q = query.trim().lowercase()
        games.filter { game ->
            (filter == "all" || game.portType == filter) &&
                (q.isEmpty() || game.searchBlob.contains(q))
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text(
            "Search",
            style = AppText.LargeTitle,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .glass(shape = RoundedCornerShape(18.dp), shadowElevation = 0.dp)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = Ink.TextTertiary, modifier = Modifier.size(19.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search games, console, engine…", style = AppText.Body.copy(fontSize = 15.sp), color = Ink.TextTertiary)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = AppText.Body.copy(fontSize = 15.sp, color = Ink.TextPrimary),
                    cursorBrush = SolidColor(Ink.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear",
                    tint = Ink.TextTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { query = "" },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filters.forEach { (key, label) ->
                FilterPill(
                    label = label,
                    selected = filter == key,
                    onClick = { filter = key },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Text(
                    if (games.isEmpty()) "Catalog is empty — check your feed." else "No port matches “$query”.",
                    style = AppText.Body,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        onClick = { onOpen(game) },
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth(),
                        cardWidth = Dp.Unspecified,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(260),
        label = "pillBg",
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color(0xFF0A0A12) else Ink.TextSecondary,
        animationSpec = tween(220),
        label = "pillText",
    )
    Box(
        modifier = Modifier
            .pressScale(pressedScale = 0.94f, interactionSource = interaction)
            .clip(CircleShape)
            .background(background)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = AppText.Caption.copy(fontSize = 12.sp),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
