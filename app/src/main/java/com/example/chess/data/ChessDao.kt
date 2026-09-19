package com.example.chess.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChessDao {

    @Query("SELECT * FROM game_records ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records WHERE userGoogleId = :googleId ORDER BY timestamp DESC")
    fun getGamesForUser(googleId: String): Flow<List<GameRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameRecordEntity): Long

    @Query("DELETE FROM game_records WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Query("DELETE FROM game_records WHERE userGoogleId = :googleId")
    suspend fun clearGamesForUser(googleId: String)

    @Query("SELECT * FROM user_profiles WHERE googleId = :googleId LIMIT 1")
    fun getUserProfile(googleId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE googleId = :googleId LIMIT 1")
    suspend fun getUserProfileDirect(googleId: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles ORDER BY lastSyncedTimestamp DESC")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>
}
