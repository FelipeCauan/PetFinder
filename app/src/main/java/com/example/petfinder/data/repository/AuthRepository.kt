package com.example.petfinder.data.repository

import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.UserEntity
import java.security.MessageDigest

/**
 * Repositório responsável pelo cadastro, login e controole de sessão offline do usuári.
 */
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {

    suspend fun getLoggedInUser(): UserEntity? {
        val userId = sessionManager.getUserId() ?: return null
        val user = userDao.getUserById(userId)
        if (user == null) {
            sessionManager.clearSession()
        }
        return user
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("E-mail ou senha inválidos."))

        if (user.passwordHash != hashPassword(password)) {
            return Result.failure(IllegalArgumentException("E-mail ou senha inválidos."))
        }

        sessionManager.saveUserId(user.id)
        return Result.success(user)
    }

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        cpf: String,
        phone: String,
        cep: String,
        street: String,
        number: String,
        city: String,
        uf: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<UserEntity> {
        val normalizedEmail = email.trim().lowercase()
        if (userDao.getUserByEmail(normalizedEmail) != null) {
            return Result.failure(IllegalStateException("Já existe uma conta cadastrada com este e-mail."))
        }

        val newUser = UserEntity(
            fullName = fullName.trim(),
            email = normalizedEmail,
            passwordHash = hashPassword(password),
            cpf = cpf.filter { it.isDigit() },
            phone = phone.filter { it.isDigit() },
            cep = cep.filter { it.isDigit() },
            street = street.trim(),
            number = number.trim(),
            city = city.trim(),
            uf = uf.trim().uppercase(),
            latitude = latitude,
            longitude = longitude
        )

        val insertedId = userDao.insertUser(newUser)
        val savedUser = newUser.copy(id = insertedId)
        sessionManager.saveUserId(insertedId)
        return Result.success(savedUser)
    }

    suspend fun updateProfile(
        userId: Long,
        fullName: String,
        email: String,
        cpf: String,
        phone: String,
        cep: String,
        street: String,
        number: String,
        city: String,
        uf: String,
        latitude: Double?,
        longitude: Double?,
        photoPath: String?,
        newPassword: String? = null
    ): Result<UserEntity> {
        val currentUser = userDao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("Usuário não encontrado."))

        val normalizedEmail = email.trim().lowercase()
        val existingWithEmail = userDao.getUserByEmail(normalizedEmail)
        if (existingWithEmail != null && existingWithEmail.id != userId) {
            return Result.failure(IllegalStateException("Já existe uma conta cadastrada com este e-mail."))
        }

        val updatedPasswordHash = if (!newPassword.isNullOrBlank()) {
            hashPassword(newPassword)
        } else {
            currentUser.passwordHash
        }

        val updatedUser = currentUser.copy(
            fullName = fullName.trim(),
            email = normalizedEmail,
            passwordHash = updatedPasswordHash,
            cpf = cpf.filter { it.isDigit() },
            phone = phone.filter { it.isDigit() },
            cep = cep.filter { it.isDigit() },
            street = street.trim(),
            number = number.trim(),
            city = city.trim(),
            uf = uf.trim().uppercase(),
            latitude = latitude,
            longitude = longitude,
            photoPath = photoPath?.takeIf { it.isNotBlank() }
        )

        userDao.updateUser(updatedUser)
        return Result.success(updatedUser)
    }

    suspend fun deleteAccount(userId: Long): Result<Unit> = runCatching {
        val deletedRows = userDao.deleteUserById(userId)
        if (deletedRows <= 0) {
            throw IllegalStateException("Conta não encontrada.")
        }
        sessionManager.clearSession()
    }

    fun logout() {
        sessionManager.clearSession()
    }

    companion object {
        internal fun hashPassword(password: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
