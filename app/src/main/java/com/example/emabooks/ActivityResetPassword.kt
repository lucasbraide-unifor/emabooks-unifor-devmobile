package com.example.emabooks

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityResetPassword : AppCompatActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSaveNewPassword: Button

    private lateinit var tvErrorFillAll: TextView
    private lateinit var tvErrorMinChars: TextView
    private lateinit var tvErrorNotMatch: TextView

    private lateinit var fb: FirebaseFirestore
    private val collectionName = "users"

    private val userId by lazy { intent.getStringExtra("userId") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        fb = Firebase.firestore

        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSaveNewPassword = findViewById(R.id.btnSaveNewPassword)

        tvErrorFillAll = findViewById(R.id.tvErrorFillAll)
        tvErrorMinChars = findViewById(R.id.tvErrorMinChars)
        tvErrorNotMatch = findViewById(R.id.tvErrorNotMatch)

        btnSaveNewPassword.setOnClickListener { saveNewPassword() }
    }

    private fun saveNewPassword() {
        hideAllErrors()

        val newPass = etNewPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()

        var hasError = false
        if (newPass.isEmpty() || confirm.isEmpty()) {
            tvErrorFillAll.visibility = View.VISIBLE
            hasError = true
        }
        if (newPass.length < 6) {
            tvErrorMinChars.visibility = View.VISIBLE
            hasError = true
        }
        if (newPass != confirm) {
            tvErrorNotMatch.visibility = View.VISIBLE
            hasError = true
        }
        if (hasError) return

        if (userId.isNullOrBlank()) {
            Toast.makeText(this, "Sessão inválida. Refaça o processo.", Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true)

        // ⚠️ Ambiente de testes: salvando a senha em texto (não faça isso em produção).
        fb.collection(collectionName)
            .document(userId!!)
            .update(
                mapOf(
                    "senha_demo" to newPass,
                    "senhaAtualizadaEm" to Timestamp.now()
                )
            )
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Senha alterada com sucesso!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Não foi possível alterar a senha. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun hideAllErrors() {
        tvErrorFillAll.visibility = View.GONE
        tvErrorMinChars.visibility = View.GONE
        tvErrorNotMatch.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        btnSaveNewPassword.isEnabled = !loading
        etNewPassword.isEnabled = !loading
        etConfirmPassword.isEnabled = !loading
        btnSaveNewPassword.text = if (loading) "Salvando..." else "Salvar Nova Senha"
    }
}