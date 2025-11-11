package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityForgotPassword : AppCompatActivity() {

    private lateinit var etForgotEmail: EditText
    private lateinit var btnSendResetLink: Button
    private lateinit var tvForgotError: TextView
    private lateinit var tvBackToLogin: TextView

    private lateinit var fb: FirebaseFirestore
    private val collectionName = "user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        fb = Firebase.firestore

        etForgotEmail = findViewById(R.id.etForgotEmail)
        btnSendResetLink = findViewById(R.id.btnSendResetLink)
        tvForgotError = findViewById(R.id.tvForgotError)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnSendResetLink.setOnClickListener { verifyEmailAndProceed() }
        tvBackToLogin.setOnClickListener { finish() }
    }

    private fun verifyEmailAndProceed() {
        showError(null)

        val email = etForgotEmail.text.toString().trim()
        if (email.isEmpty()) return showError("Informe o e-mail")
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return showError("Digite um e-mail válido")

        setLoading(true)

        fb.collection(collectionName)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                setLoading(false)
                if (snap.isEmpty) {
                    showError("E-mail não encontrado")
                    return@addOnSuccessListener
                }

                val doc = snap.documents.first()
                val userId = doc.id

                // Sem e-mail: já leva direto para a tela de redefinição
                val i = Intent(this, ActivityResetPassword::class.java)
                i.putExtra("userId", userId)
                startActivity(i)
            }
            .addOnFailureListener {
                setLoading(false)
                showError("Falha ao verificar e-mail. Tente novamente.")
            }
    }

    private fun showError(msg: String?) {
        if (msg.isNullOrBlank()) {
            tvForgotError.visibility = View.GONE
        } else {
            tvForgotError.text = msg
            tvForgotError.visibility = View.VISIBLE
        }
    }

    private fun setLoading(loading: Boolean) {
        btnSendResetLink.isEnabled = !loading
        etForgotEmail.isEnabled = !loading
        btnSendResetLink.text = if (loading) "Verificando..." else "Enviar Link"
    }
}