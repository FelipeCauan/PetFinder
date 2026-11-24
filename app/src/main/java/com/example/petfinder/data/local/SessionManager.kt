package com.example.petfinder.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Gerencia a sessão local do usuário autenticado usando SharedPreferences.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, NO_USER)
        return if (id == NO_USER) null else id
    }

    fun clearSession() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    companion object {
        private const val PREFS_NAME = "petfinder_session"
        private const val KEY_USER_ID = "current_user_id"
        private const val NO_USER = -1L
    }
}
