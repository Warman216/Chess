package com.example.chess.data

import com.example.chess.ai.AiDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.pow
import kotlin.math.roundToInt

class ChessRepository(private val dao: ChessDao) {

    fun getGamesForUser(googleId: String): Flow<List<GameRecordEntity>> {
        return dao.getGamesForUser(googleId)
    }

    fun getAllGames(): Flow<List<GameRecordEntity>> {
        return dao.getAllGames()
    }

    fun getUserProfile(googleId: String): Flow<UserProfileEntity?> {
        return dao.getUserProfile(googleId)
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        dao.insertOrUpdateUserProfile(profile)
    }

    suspend fun recordGame(
        userGoogleId: String,
        userEmail: String,
        playerColor: String,
        difficulty: AiDifficulty,
        result: String, // "WIN", "LOSS", "DRAW"
        winReason: String,
        moveCount: Int,
        durationSeconds: Long,
        movesSan: List<String>,
        fenFinal: String
    ): GameRecordEntity {
        // Calculate Elo change
        val currentProfile = dao.getUserProfileDirect(userGoogleId)
        val currentRating = currentProfile?.rating ?: 1200
        val opponentRating = difficulty.ratingEstimate

        val ratingChange = calculateEloChange(currentRating, opponentRating, result)
        val newRating = maxOf(100, currentRating + ratingChange)
        val peakRating = maxOf(newRating, currentProfile?.peakRating ?: 1200)

        val updatedProfile = UserProfileEntity(
            googleId = userGoogleId,
            displayName = currentProfile?.displayName ?: (if (userEmail.contains("@")) userEmail.substringBefore("@") else "Chess Player"),
            email = userEmail,
            avatarUrl = currentProfile?.avatarUrl,
            rating = newRating,
            peakRating = peakRating,
            lastSyncedTimestamp = System.currentTimeMillis()
        )
        dao.insertOrUpdateUserProfile(updatedProfile)

        val entity = GameRecordEntity(
            userGoogleId = userGoogleId,
            userEmail = userEmail,
            playerColor = playerColor,
            aiDifficulty = difficulty.displayName,
            result = result,
            winReason = winReason,
            moveCount = moveCount,
            durationSeconds = durationSeconds,
            movesSanJson = movesSan.joinToString(","),
            fenFinal = fenFinal,
            ratingChange = ratingChange,
            timestamp = System.currentTimeMillis()
        )

        dao.insertGame(entity)
        return entity
    }

    fun getDetailedStatsForUser(googleId: String): Flow<DetailedStats> {
        return dao.getGamesForUser(googleId).map { games ->
            calculateStats(games)
        }
    }

    private fun calculateStats(games: List<GameRecordEntity>): DetailedStats {
        if (games.isEmpty()) return DetailedStats()

        val totalGames = games.size
        var wins = 0
        var losses = 0
        var draws = 0

        var whiteW = 0; var whiteL = 0; var whiteD = 0
        var blackW = 0; var blackL = 0; var blackD = 0

        var casualW = 0; var casualL = 0; var casualD = 0
        var clubW = 0; var clubL = 0; var clubD = 0
        var gmW = 0; var gmL = 0; var gmD = 0

        var winCheckmate = 0
        var winResign = 0
        var lossCheckmate = 0
        var lossResign = 0
        var drawsStalemate = 0

        var totalMoves = 0
        var totalDuration = 0L

        // Sorted by timestamp ascending for streaks
        val sortedAsc = games.sortedBy { it.timestamp }
        var currentStreak = 0
        var longestWinStreak = 0
        var tempWinStreak = 0

        for (g in sortedAsc) {
            when (g.result) {
                "WIN" -> {
                    wins++
                    if (currentStreak >= 0) currentStreak++ else currentStreak = 1
                    tempWinStreak++
                    if (tempWinStreak > longestWinStreak) longestWinStreak = tempWinStreak

                    if (g.winReason.contains("Checkmate", ignoreCase = true)) winCheckmate++
                    if (g.winReason.contains("Resign", ignoreCase = true)) winResign++
                }
                "LOSS" -> {
                    losses++
                    if (currentStreak <= 0) currentStreak-- else currentStreak = -1
                    tempWinStreak = 0

                    if (g.winReason.contains("Checkmate", ignoreCase = true)) lossCheckmate++
                    if (g.winReason.contains("Resign", ignoreCase = true)) lossResign++
                }
                else -> {
                    draws++
                    currentStreak = 0
                    tempWinStreak = 0
                    if (g.winReason.contains("Stalemate", ignoreCase = true)) drawsStalemate++
                }
            }

            // By Color
            if (g.playerColor == "WHITE") {
                when (g.result) {
                    "WIN" -> whiteW++
                    "LOSS" -> whiteL++
                    else -> whiteD++
                }
            } else {
                when (g.result) {
                    "WIN" -> blackW++
                    "LOSS" -> blackL++
                    else -> blackD++
                }
            }

            // By Difficulty
            when {
                g.aiDifficulty.contains("Casual", ignoreCase = true) -> {
                    when (g.result) {
                        "WIN" -> casualW++
                        "LOSS" -> casualL++
                        else -> casualD++
                    }
                }
                g.aiDifficulty.contains("Club", ignoreCase = true) -> {
                    when (g.result) {
                        "WIN" -> clubW++
                        "LOSS" -> clubL++
                        else -> clubD++
                    }
                }
                else -> {
                    when (g.result) {
                        "WIN" -> gmW++
                        "LOSS" -> gmL++
                        else -> gmD++
                    }
                }
            }

            totalMoves += g.moveCount
            totalDuration += g.durationSeconds
        }

        val winRate = if (totalGames > 0) (wins.toFloat() / totalGames) * 100f else 0f
        val avgMoves = if (totalGames > 0) totalMoves.toFloat() / totalGames else 0f
        val avgDuration = if (totalGames > 0) totalDuration / totalGames else 0L

        return DetailedStats(
            totalGames = totalGames,
            wins = wins,
            losses = losses,
            draws = draws,
            winRate = winRate,
            currentStreak = currentStreak,
            longestWinStreak = longestWinStreak,
            whiteRecord = RecordBreakdown(whiteW, whiteL, whiteD),
            blackRecord = RecordBreakdown(blackW, blackL, blackD),
            casualRecord = RecordBreakdown(casualW, casualL, casualD),
            clubRecord = RecordBreakdown(clubW, clubL, clubD),
            grandmasterRecord = RecordBreakdown(gmW, gmL, gmD),
            winByCheckmate = winCheckmate,
            winByResignation = winResign,
            lossByCheckmate = lossCheckmate,
            lossByResignation = lossResign,
            drawsByStalemate = drawsStalemate,
            averageMoves = avgMoves,
            averageDurationSeconds = avgDuration
        )
    }

    private fun calculateEloChange(playerRating: Int, opponentRating: Int, result: String): Int {
        val k = 32
        val expectedScore = 1.0 / (1.0 + 10.0.pow((opponentRating - playerRating).toDouble() / 400.0))
        val actualScore = when (result) {
            "WIN" -> 1.0
            "LOSS" -> 0.0
            else -> 0.5
        }
        val change = (k * (actualScore - expectedScore)).roundToInt()
        return if (result == "WIN" && change < 5) 5 else if (result == "LOSS" && change > -5) -5 else change
    }

    suspend fun clearHistory(googleId: String) {
        dao.clearGamesForUser(googleId)
    }
}
