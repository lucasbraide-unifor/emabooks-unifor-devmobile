package com.example.emabooks

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Ajusta a fonte ANTES de inflar layout
        val config = newBase.resources.configuration
        val newConfig = android.content.res.Configuration(config)
        newConfig.fontScale = AppConfigManager.getFontScale(newBase)

        val context = newBase.createConfigurationContext(newConfig)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica tema claro/escuro antes de criar a Activity
        AppConfigManager.applyUserConfig(this)
        super.onCreate(savedInstanceState)
    }
}