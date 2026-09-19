package com.example.chess.data

data class RecordBreakdown(
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0
) {
    val total: Int get() = wins + losses + draws
    val winRate: Float get() = if (total > 0) (wins.toFloat() / total) * 100f else 0f
}

data class DetailedStats(
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val winRate: Float = 0f,
    val currentStreak: Int = 0, // Positive for win streak, negative for loss streak
    val longestWinStreak: Int = 0,
    val rating: Int = 1200,
    val peakRating: Int = 1200,
    val whiteRecord: RecordBreakdown = RecordBreakdown(),
    val blackRecord: RecordBreakdown = RecordBreakdown(),
    val casualRecord: RecordBreakdown = RecordBreakdown(),
    val clubRecord: RecordBreakdown = RecordBreakdown(),
    val grandmasterRecord: RecordBreakdown = RecordBreakdown(),
    val winByCheckmate: Int = 0,
    val winByResignation: Int = 0,
    val lossByCheckmate: Int = 0,
    val lossByResignation: Int = 0,
    val drawsByStalemate: Int = 0,
    val averageMoves: Float = 0f,
    val averageDurationSeconds: Long = 0L
)
