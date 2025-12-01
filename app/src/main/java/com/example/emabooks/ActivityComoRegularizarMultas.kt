package com.example.emabooks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class ActivityComoRegularizarMultas : AppCompatActivity() {

    private lateinit var toolbarComoRegularizarMultas: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_como_regularizar_multas)

        toolbarComoRegularizarMultas = findViewById(R.id.toolbarComoRegularizarMultas)
        toolbarComoRegularizarMultas.setNavigationOnClickListener {
            finish()
        }
    }
}