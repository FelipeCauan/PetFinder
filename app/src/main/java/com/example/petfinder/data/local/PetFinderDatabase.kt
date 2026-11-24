package com.example.petfinder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.UserEntity

/**
 * Banco de dados local Room do aplicativo PetFinder (arquitetura offline-first).
 */
@Database(
    entities = [UserEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PetFinderDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: PetFinderDatabase? = null

        fun getInstance(context: Context): PetFinderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PetFinderDatabase::class.java,
                    "petfinder_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
