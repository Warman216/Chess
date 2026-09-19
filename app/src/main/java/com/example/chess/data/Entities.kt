package com.example.chess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userGoogleId: String,
    val userEmail: String,
    val playerColor: String, // "WHITE" or "BLACK"
    val aiDifficulty: String, // "Casual", "Club Player", "Grandmaster Gemini"
    val result: String, // "WIN", "LOSS", "DRAW"
    val winReason: String, // "Checkmate", "Resignation", "Stalemate", "50 Moves", etc.
    val moveCount: Int,
    val durationSeconds: Long,
    val movesSanJson: String, // Comma-separated or JSON list of SAN moves
    val fenFinal: String,
    val ratingChange: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val googleId: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val rating: Int = 1200,
    val peakRating: Int = 1200,
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
)
