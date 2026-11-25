package com.example.petfinder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.petfinder.data.local.dao.PetDao
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.local.entity.UserEntity

/**
 * Banco de dados local Room do aplicativo PetFinder (arquitetura offline-first).
 */
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN photoPath TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): PetFinderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PetFinderDatabase::class.java,
                    "petfinder_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
