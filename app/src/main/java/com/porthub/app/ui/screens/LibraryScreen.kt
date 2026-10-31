package com.porthub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.porthub.app.data.Game
import com.porthub.app.ui.components.GameCard
import com.porthub.app.ui.components.glass
import com.porthub.app.ui.theme.AppText
import com.porthub.app.ui.theme.Ink

@Composable
fun LibraryScreen(
    games: List<Game>,
    favoriteIds: Set<String>,
    onOpen: (Game) -> Unit,
) {
    val saved = games.filter { it.id in favoriteIds }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text(
            "Library",
            style = AppText.LargeTitle,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (saved.isEmpty()) "No saved ports" else "${saved.size} saved",
            style = AppText.Caption,
            color = Ink.TextTertiary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(16.dp))

        if (saved.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .glass(shape = RoundedCornerShape(22.dp), shadowElevation = 0.dp)
                        .padding(20.dp),
                ) {
                    Text("Your shelf is empty", style = AppText.CardTitle)
                    Spacer(Modifier.height(6.dp))
                    Text("Tap the heart on any port to keep it here. Saved ports also stay available offline.", style = AppText.Body)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(saved, key = { it.id }) { game ->
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
