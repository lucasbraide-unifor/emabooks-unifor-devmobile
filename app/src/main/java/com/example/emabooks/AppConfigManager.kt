package com.example.emabooks

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object AppConfigManager {

    private const val PREFS_NAME = "ema_books_prefs"
    private const val KEY_FONT_SIZE_INDEX = "font_size_index"
    private const val KEY_CONTRAST_INDEX = "contrast_index"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ---------------- FONT SIZE ----------------

    fun getFontSizeIndex(context: Context): Int {
        return getPrefs(context).getInt(KEY_FONT_SIZE_INDEX, 0) // 0 = Padrão
    }

    fun setFontSizeIndex(context: Context, index: Int) {
        getPrefs(context).edit().putInt(KEY_FONT_SIZE_INDEX, index).apply()
    }

    // Mapeia índice para escala (ajuste como quiser)
    fun getFontScale(context: Context): Float {
        return when (getFontSizeIndex(context)) {
            1 -> 1.10f // Médio
            2 -> 1.25f // Grande
            else -> 1.0f // Padrão
        }
    }

    // ---------------- CONTRASTE / TEMA ----------------

    fun getContrastIndex(context: Context): Int {
        return getPrefs(context).getInt(KEY_CONTRAST_INDEX, 0) // 0 = Padrão (claro), 1 = Alto contraste / escuro
    }

    fun setContrastIndex(context: Context, index: Int) {
        getPrefs(context).edit().putInt(KEY_CONTRAST_INDEX, index).apply()
    }

    private fun applyContrast(context: Context) {
        val idx = getContrastIndex(context)
        val desiredMode = if (idx == 0) {
            AppCompatDelegate.MODE_NIGHT_NO   // modo dia
        } else {
            AppCompatDelegate.MODE_NIGHT_YES  // modo escuro / alto contraste
        }

        if (AppCompatDelegate.getDefaultNightMode() != desiredMode) {
            AppCompatDelegate.setDefaultNightMode(desiredMode)
        }
    }

    // ---------------- NOTIFICAÇÕES ----------------

    fun isNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    // ---------------- APLICAÇÃO GLOBAL ----------------

    fun applyUserConfig(activity: Activity) {
        // 1. Modo claro/escuro
        applyContrast(activity)

        // 2. Escala de fonte
        applyFontScale(activity)
    }

    private fun applyFontScale(activity: Activity) {
        val resources = activity.resources
        val configuration = resources.configuration
        val targetScale = getFontScale(activity)

        if (configuration.fontScale == targetScale) return

        configuration.fontScale = targetScale
        val metrics = resources.displayMetrics
        resources.updateConfiguration(configuration, metrics)
    }
}