package com.example.petfinder.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * Gerencia a sessão local do usuário autenticado usando SharedPreferences
 * e mantém a localização GPS temporária da sessão apenas em memória.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _currentUserId = MutableStateFlow(readStoredUserId())
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val _temporaryLocation = MutableStateFlow<GeoPoint?>(null)
    val temporaryLocation: StateFlow<GeoPoint?> = _temporaryLocation.asStateFlow()

    fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
        _currentUserId.value = userId
    }

    fun getUserId(): Long? = _currentUserId.value

    fun setTemporaryLocation(latitude: Double, longitude: Double) {
        _temporaryLocation.value = GeoPoint(latitude, longitude)
    }

    fun clearTemporaryLocation() {
        _temporaryLocation.value = null
    }

    fun clearSession() {
        prefs.edit().remove(KEY_USER_ID).apply()
        _currentUserId.value = null
        _temporaryLocation.value = null
    }

    private fun readStoredUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, NO_USER)
        return if (id == NO_USER) null else id
    }

    companion object {
        private const val PREFS_NAME = "petfinder_session"
        private const val KEY_USER_ID = "current_user_id"
        private const val NO_USER = -1L
    }
}
