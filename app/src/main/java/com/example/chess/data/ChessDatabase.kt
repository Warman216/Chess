package com.example.chess.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GameRecordEntity::class, UserProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun chessDao(): ChessDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getInstance(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "chess_database.db"
                ).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
