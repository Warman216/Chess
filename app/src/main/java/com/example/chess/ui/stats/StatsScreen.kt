package com.example.chess.ui.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.auth.GoogleUser
import com.example.chess.data.DetailedStats
import com.example.chess.data.RecordBreakdown

@Composable
fun StatsScreen(
    stats: DetailedStats,
    user: GoogleUser,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("stats_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Google Account Sync Banner
            GoogleAccountSyncHeader(user)
        }

        item {
            // Main Record Hero Card
            OverallRecordCard(stats)
        }

        item {
            // Key metrics: Streak, Rating, Peak, Avg Moves
            KeyMetricsGrid(stats)
        }

        item {
            // Color Breakdown (White vs Black)
            ColorPerformanceCard(stats)
        }

        item {
            // AI Difficulty Breakdown
            DifficultyPerformanceCard(stats)
        }

        item {
            // Win / Loss Methods (Checkmate, Resignation, Stalemate)
            OutcomeTypesCard(stats)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GoogleAccountSyncHeader(user: GoogleUser) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4285F4), Color(0xFF34A853))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Synced",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = if (user.isSignedIn) "Google Account: ${user.email}" else "Local Profile (Sign in to sync)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OverallRecordCard(stats: DetailedStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Win-Loss Record",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "%.1f%% Win Rate".format(stats.winRate),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Segmented Bar
            val total = stats.totalGames
            val winWeight = if (total > 0) stats.wins.toFloat() / total else 0.33f
            val lossWeight = if (total > 0) stats.losses.toFloat() / total else 0.33f
            val drawWeight = if (total > 0) stats.draws.toFloat() / total else 0.34f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                if (stats.wins > 0 || total == 0) {
                    Box(
                        modifier = Modifier
                            .weight(winWeight.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(Color(0xFF10B981))
                    )
                }
                if (stats.draws > 0 || total == 0) {
                    Box(
                        modifier = Modifier
                            .weight(drawWeight.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(Color(0xFFF59E0B))
                    )
                }
                if (stats.losses > 0 || total == 0) {
                    Box(
                        modifier = Modifier
                            .weight(lossWeight.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(Color(0xFFEF4444))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metric Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatCounter(
                    count = stats.wins,
                    label = "Wins",
                    color = Color(0xFF10B981)
                )
                StatCounter(
                    count = stats.losses,
                    label = "Losses",
                    color = Color(0xFFEF4444)
                )
                StatCounter(
                    count = stats.draws,
                    label = "Draws",
                    color = Color(0xFFF59E0B)
                )
                StatCounter(
                    count = stats.totalGames,
                    label = "Total",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatCounter(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KeyMetricsGrid(stats: DetailedStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Elo Rating
        MetricPillCard(
            title = "Chess Rating",
            value = "${stats.rating}",
            subtitle = "Peak: ${stats.peakRating}",
            icon = Icons.AutoMirrored.Filled.ShowChart,
            accentColor = Color(0xFF6366F1),
            modifier = Modifier.weight(1f)
        )

        // Streak
        val streakText = when {
            stats.currentStreak > 0 -> "${stats.currentStreak} Wins"
            stats.currentStreak < 0 -> "${-stats.currentStreak} Losses"
            else -> "None"
        }
        MetricPillCard(
            title = "Current Streak",
            value = streakText,
            subtitle = "Best: ${stats.longestWinStreak} Wins",
            icon = Icons.Default.Whatshot,
            accentColor = if (stats.currentStreak > 0) Color(0xFFF59E0B) else Color(0xFF94A3B8),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricPillCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorPerformanceCard(stats: DetailedStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance by Piece Color",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            ColorRecordRow(
                colorName = "As White ♔",
                record = stats.whiteRecord
            )
            Spacer(modifier = Modifier.height(10.dp))
            ColorRecordRow(
                colorName = "As Black ♚",
                record = stats.blackRecord
            )
        }
    }
}

@Composable
private fun ColorRecordRow(colorName: String, record: RecordBreakdown) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = colorName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${record.wins}W - ${record.losses}L - ${record.draws}D  (%.0f%%)".format(record.winRate),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (record.total > 0) record.wins.toFloat() / record.total else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF10B981),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun DifficultyPerformanceCard(stats: DetailedStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance vs AI Difficulties",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            DifficultyRow("Casual AI", stats.casualRecord)
            Spacer(modifier = Modifier.height(10.dp))
            DifficultyRow("Club Player AI", stats.clubRecord)
            Spacer(modifier = Modifier.height(10.dp))
            DifficultyRow("Grandmaster Gemini ✨", stats.grandmasterRecord)
        }
    }
}

@Composable
private fun DifficultyRow(name: String, record: RecordBreakdown) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${record.wins}W - ${record.losses}L - ${record.draws}D",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (record.total > 0) record.wins.toFloat() / record.total else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun OutcomeTypesCard(stats: DetailedStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Outcome Breakdown & Length",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Checkmates Delivered", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.winByCheckmate}", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Resignations Won", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.winByResignation}", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Checkmates Suffered", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.lossByCheckmate}", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Stalemates", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.drawsByStalemate}", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Average Moves / Game", style = MaterialTheme.typography.bodyMedium)
                Text("%.1f".format(stats.averageMoves), fontWeight = FontWeight.Bold)
            }
        }
    }
}
