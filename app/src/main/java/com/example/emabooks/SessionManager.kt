package com.example.emabooks

import android.content.Context

object SessionManager {

    private const val PREFS_NAME = "emaBooks_prefs"
    private const val KEY_USER_ID = "user_id"

    fun salvarUsuarioId(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun obterUsuarioId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, null)
    }

    fun limparSessao(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_USER_ID)
            .apply()
    }
}