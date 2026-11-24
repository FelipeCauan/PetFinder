package com.example.petfinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.petfinder.data.local.entity.UserEntity

/**
 * DAO responsável pelas operações de persistência de usuários no Room.
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?
}
