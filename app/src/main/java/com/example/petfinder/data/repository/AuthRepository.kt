package com.example.petfinder.data.repository

import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.UserEntity
import java.security.MessageDigest

/**
 * Repositório responsável pelo cadastro, login e controle de sessão offline do usuário.
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
