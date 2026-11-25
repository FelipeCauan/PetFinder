package com.example.petfinder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.petfinder.data.local.dao.PetDao
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, PetEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PetFinderDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: PetFinderDatabase? = null

        fun getInstance(context: Context): PetFinderDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PetFinderDatabase::class.java,
                    "petfinder_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
