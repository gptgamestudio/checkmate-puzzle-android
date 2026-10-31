package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Puzzle

@Composable
fun LevelSelectScreen(
    puzzles: List<Puzzle>,
    currentLevelIndex: Int,
    solvedIds: Set<String>,
    onSelectLevel: (Int) -> Unit,
    onBackToBoard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRangeStart by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf((currentLevelIndex / 200) * 200)
    }

    val pageSize = 200
    val currentRangeEnd = (selectedRangeStart + pageSize).coerceAtMost(puzzles.size)
    val pagePuzzles = puzzles.subList(selectedRangeStart, currentRangeEnd)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("level_select_screen"),
        color = Color(0xFF080A0B)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToBoard,
                        modifier = Modifier
                            .background(Color(0xFF141719), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF303638), RoundedCornerShape(8.dp))
                            .testTag("back_to_board_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Board",
                            tint = Color(0xFFE5B94F)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Level Selection (1 - 2,000)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "${solvedIds.size} of ${puzzles.size} Checkmate Puzzles Completed",
                            fontSize = 12.sp,
                            color = Color(0xFFE5B94F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Range Tabs (1-200, 201-400, ... 1801-2000)
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalSteps = (puzzles.size + pageSize - 1) / pageSize
                items(totalSteps) { stepIndex ->
                    val rangeStart = stepIndex * pageSize
                    val rangeEnd = (rangeStart + pageSize).coerceAtMost(puzzles.size)
                    val isSelected = rangeStart == selectedRangeStart

                    Button(
                        onClick = { selectedRangeStart = rangeStart },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFFEAB308) else Color(0xFF1E2230),
                            contentColor = if (isSelected) Color(0xFF0F1015) else Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${rangeStart + 1}-$rangeEnd",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Level Grid (4 columns in landscape)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(pagePuzzles) { localIndex, puzzle ->
                    val actualIndex = selectedRangeStart + localIndex
                    val isCurrent = actualIndex == currentLevelIndex
                    val isSolved = solvedIds.contains(puzzle.id)
                    // Level is unlocked if it is level 1 or previous level is solved
                    val isUnlocked = actualIndex == 0 || solvedIds.contains(puzzles.getOrNull(actualIndex - 1)?.id ?: "") || isSolved

                    val diffColor = when (puzzle.difficulty) {
                        "Beginner" -> Color(0xFF22C55E)
                        "Intermediate" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEC4899)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isUnlocked) {
                                onSelectLevel(actualIndex)
                            }
                            .testTag("level_card_$actualIndex"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isCurrent -> Color(0xFF242A3E)
                                isUnlocked -> Color(0xFF181A25)
                                else -> Color(0xFF111218)
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = when {
                                isCurrent -> Color(0xFFEAB308)
                                isSolved -> Color(0xFF22C55E)
                                else -> Color(0xFF272A38)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Level number badge & status icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LEVEL ${actualIndex + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isCurrent) Color(0xFFEAB308) else Color(0xFF94A3B8)
                                )

                                if (isSolved) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Solved",
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (!isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = puzzle.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isUnlocked) Color.White else Color(0xFF64748B),
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = puzzle.theme,
                                fontSize = 11.sp,
                                color = diffColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
