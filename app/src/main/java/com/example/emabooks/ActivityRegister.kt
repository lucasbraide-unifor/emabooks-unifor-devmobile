package com.example.emabooks

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityRegister : AppCompatActivity() {

    // Views
    private lateinit var etNomeCompleto: EditText
    private lateinit var etEmailCadastro: EditText
    private lateinit var etMatricula: EditText
    private lateinit var etSenhaCadastro: EditText
    private lateinit var etConfirmaSenha: EditText
    private lateinit var tvErroCadastro: TextView
    private lateinit var btnCadastrar: Button
    private lateinit var tvFacaLogin: TextView

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val collectionName = "pessoa" // mantendo o padrão salvo no contexto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Init Firestore
        fb = Firebase.firestore

        // Bind
        etNomeCompleto = findViewById(R.id.etNomeCompleto)
        etEmailCadastro = findViewById(R.id.etEmailCadastro)
        etMatricula = findViewById(R.id.etMatricula)
        etSenhaCadastro = findViewById(R.id.etSenhaCadastro)
        etConfirmaSenha = findViewById(R.id.etConfirmaSenha)
        tvErroCadastro = findViewById(R.id.tvErroCadastro)
        btnCadastrar = findViewById(R.id.btnCadastrar)
        tvFacaLogin = findViewById(R.id.tvFacaLogin)

        btnCadastrar.setOnClickListener { attemptRegister() }
        tvFacaLogin.setOnClickListener {
            // Veio do Login -> só fechar para voltar
            finish()
        }
    }

    private fun attemptRegister() {
        // Limpa mensagem anterior
        showError(null)

        val nome = etNomeCompleto.text.toString().trim()
        val email = etEmailCadastro.text.toString().trim()
        val matricula = etMatricula.text.toString().trim()
        val senha = etSenhaCadastro.text.toString()
        val confirma = etConfirmaSenha.text.toString()

        // Validações conforme requisitos
        if (nome.isEmpty() || email.isEmpty() || matricula.isEmpty() || senha.isEmpty() || confirma.isEmpty()) {
            return showError("Preencha todos os campos")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return showError("Insira um e-mail e senha válidos")
        }
        if (senha.length < 6) {
            return showError("Insira um e-mail e senha válidos")
        }
        if (senha != confirma) {
            return showError("Senhas não coincidem")
        }

        setLoading(true)

        // Checa duplicidade (email OU matrícula)
        val emailTask = fb.collection(collectionName)
            .whereEqualTo("email", email)
            .limit(1)
            .get()

        val matriculaTask = fb.collection(collectionName)
            .whereEqualTo("matricula", matricula)
            .limit(1)
            .get()

        Tasks.whenAllSuccess<Any>(emailTask, matriculaTask)
            .addOnSuccessListener { results ->
                val emailSnap = emailTask.result
                val matSnap = matriculaTask.result

                val emailJaExiste = emailSnap != null && !emailSnap.isEmpty
                val matriculaJaExiste = matSnap != null && !matSnap.isEmpty

                if (emailJaExiste || matriculaJaExiste) {
                    setLoading(false)
                    showError("Usuário já existente")
                    return@addOnSuccessListener
                }

                // Monta o documento do usuário
                val userDoc = hashMapOf(
                    "nome" to nome,
                    "email" to email,
                    "matricula" to matricula,
                    // ⚠️ NÃO salve senha em claro em produção — usar Firebase Auth.
                    "senha_demo" to senha, // apenas para testes locais
                    "createdAt" to Timestamp.now()
                )

                fb.collection(collectionName)
                    .add(userDoc)
                    .addOnSuccessListener {
                        setLoading(false)
                        Toast.makeText(this, "Cadastro realizado com sucesso", Toast.LENGTH_SHORT).show()
                        // Redireciona para a tela de Login
                        finish()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        showError("Erro ao salvar cadastro. Tente novamente.")
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                showError("Erro ao validar duplicidade. Verifique sua conexão.")
            }
    }

    private fun showError(msg: String?) {
        if (msg.isNullOrBlank()) {
            tvErroCadastro.visibility = View.GONE
        } else {
            tvErroCadastro.text = msg
            tvErroCadastro.visibility = View.VISIBLE
        }
    }

    private fun setLoading(loading: Boolean) {
        btnCadastrar.isEnabled = !loading
        tvFacaLogin.isEnabled = !loading
        // Poderia trocar texto do botão para "Aguarde..." se quiser:
        // btnCadastrar.text = if (loading) "Aguarde..." else "Cadastrar"
    }
}