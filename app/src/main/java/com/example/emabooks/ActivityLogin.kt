package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityLogin : AppCompatActivity() {

    private lateinit var fb: FirebaseFirestore

    // Views
    private lateinit var etEmail: EditText
    private lateinit var etSenha: EditText
    private lateinit var tvErroLogin: TextView
    private lateinit var btnEntrar: Button
    private lateinit var tvCadastro: TextView
    private lateinit var tvEsqueciSenha: TextView

    private val collectionName = "user" // mesma coleção usada no cadastro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        fb = Firebase.firestore

        // Bind
        etEmail = findViewById(R.id.etEmail)
        etSenha = findViewById(R.id.etSenha)
        tvErroLogin = findViewById(R.id.tvErroLogin)
        btnEntrar = findViewById(R.id.btnEntrar)
        tvCadastro = findViewById(R.id.tvCadastro)
        tvEsqueciSenha = findViewById(R.id.tvEsqueciSenha)

        // Navegação para cadastro
        tvCadastro.setOnClickListener {
            startActivity(Intent(this, ActivityRegister::class.java))
        }

        // Navegação para "esqueci minha senha"
        tvEsqueciSenha.setOnClickListener {
            startActivity(Intent(this, ActivityForgotPassword::class.java))
            Toast.makeText(this, "Fluxo de recuperação em construção", Toast.LENGTH_SHORT).show()
        }

        // Login
        btnEntrar.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        showError(null) // limpa mensagem

        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString()

        // Validações (requisitos)
        if (email.isEmpty() || senha.isEmpty()) {
            showError("Preencha todos os campos")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || senha.length < 6) {
            showError("Insira um e-mail e senha válidos")
            return
        }

        setLoading(true)

        // Busca usuário por e-mail
        fb.collection(collectionName)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    setLoading(false)
                    showError("E-mail ou senha incorretos")
                    return@addOnSuccessListener
                }

                val doc = snap.documents.first()
                val senhaSalva = doc.getString("senha_demo") ?: ""

                if (senha == senhaSalva) {
                    setLoading(false)
                    Toast.makeText(this, "Login bem-sucedido", Toast.LENGTH_SHORT).show()

                    // TODO: redirecionar para a página inicial do app
                    // startActivity(Intent(this, ActivityHome::class.java))
                    // finish()

                } else {
                    setLoading(false)
                    showError("E-mail ou senha incorretos")
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                // Mostra a causa real para facilitar debug (ex.: PERMISSION_DENIED)
                showError("Falha na autenticação: ${e.message}")
            }
    }

    private fun showError(msg: String?) {
        if (msg.isNullOrBlank()) {
            tvErroLogin.visibility = View.GONE
        } else {
            tvErroLogin.text = msg
            tvErroLogin.visibility = View.VISIBLE
        }
    }

    private fun setLoading(loading: Boolean) {
        btnEntrar.isEnabled = !loading
        tvCadastro.isEnabled = !loading
        tvEsqueciSenha.isEnabled = !loading
        btnEntrar.text = if (loading) "Entrando..." else "Entrar"
    }
}