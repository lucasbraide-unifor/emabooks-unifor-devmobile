package com.example.emabooks

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

object AppConfigManager {

    private const val PREFS_NAME = "user_settings"
    private const val KEY_FONT_SIZE_INDEX = "font_size_index"
    private const val KEY_CONTRAST_INDEX = "contrast_index"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    // ---------------- PUBLIC API ----------------

    fun getFontSizeIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FONT_SIZE_INDEX, 0)
    }

    fun setFontSizeIndex(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_FONT_SIZE_INDEX, index).apply()
    }

    fun getContrastIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CONTRAST_INDEX, 0)
    }

    fun setContrastIndex(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_CONTRAST_INDEX, index).apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    /**
     * Aplica as configurações visuais na Activity.
     * Chamar em onCreate, depois do setContentView.
     */
    fun applyUserConfig(activity: AppCompatActivity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0)
            ?: return

        val fontIndex = getFontSizeIndex(activity)
        val contrastIndex = getContrastIndex(activity)

        applyFontSize(root, fontIndex)
        applyContrast(root, contrastIndex)
    }

    // ---------------- HELPERS VISUAIS ----------------

    private fun applyFontSize(view: View, fontIndex: Int) {
        val scale = when (fontIndex) {
            1 -> 1.1f  // Médio
            2 -> 1.25f // Grande
            else -> 1.0f
        }

        if (view is TextView) {
            // Mantém o textSize base e aplica um scale leve
            view.textSize = view.textSize / view.resources.displayMetrics.scaledDensity * scale
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyFontSize(view.getChildAt(i), fontIndex)
            }
        }
    }

    private fun applyContrast(view: View, contrastIndex: Int) {
        // 0 = padrão, 1 = alto contraste
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyContrast(view.getChildAt(i), contrastIndex)
            }
        }

        if (contrastIndex == 1 && view is TextView) {
            // Exemplo simples de alto contraste: texto bem escuro
            view.setTextColor(0xFF000000.toInt())
        }
        // Se quiser, dá pra sofisticar depois (backgrounds, etc.)
    }
}