package com.example.emabooks

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityAdicionarUsuario : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_USERS = "users"

    // Views
    private lateinit var toolbarAdicionarUsuario: MaterialToolbar
    private lateinit var etNomeUsuario: EditText
    private lateinit var etEmailUsuario: EditText
    private lateinit var etMatriculaUsuario: EditText
    private lateinit var switchAdmin: Switch
    private lateinit var buttonSalvar: MaterialButton
    private lateinit var buttonCancelar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_usuario)

        fb = Firebase.firestore
        bindViews()
        setupToolbar()
        setupButtons()
    }

    private fun bindViews() {
        toolbarAdicionarUsuario = findViewById(R.id.toolbarAdicionarUsuario)
        etNomeUsuario = findViewById(R.id.etNomeUsuario)
        etEmailUsuario = findViewById(R.id.etEmailUsuario)
        etMatriculaUsuario = findViewById(R.id.etMatriculaUsuario)
        switchAdmin = findViewById(R.id.switchAdmin)
        buttonSalvar = findViewById(R.id.buttonSalvar)
        buttonCancelar = findViewById(R.id.buttonCancelar)
    }

    private fun setupToolbar() {
        toolbarAdicionarUsuario.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButtons() {
        buttonCancelar.setOnClickListener {
            // apenas fecha e volta para a tela de admin de usuários
            finish()
        }

        buttonSalvar.setOnClickListener {
            validarESolicitarConfirmacao()
        }
    }

    // ---------------- VALIDAÇÃO ----------------

    private fun validarESolicitarConfirmacao() {
        val nome = etNomeUsuario.text.toString().trim()
        val email = etEmailUsuario.text.toString().trim()
        val matricula = etMatriculaUsuario.text.toString().trim()
        val isAdmin = switchAdmin.isChecked

        // validações básicas
        if (nome.isEmpty()) {
            etNomeUsuario.error = "Informe o nome completo"
            etNomeUsuario.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etEmailUsuario.error = "Informe o e-mail"
            etEmailUsuario.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailUsuario.error = "E-mail inválido"
            etEmailUsuario.requestFocus()
            return
        }

        if (matricula.isEmpty()) {
            etMatriculaUsuario.error = "Informe a matrícula"
            etMatriculaUsuario.requestFocus()
            return
        }

        if (!matricula.all { it.isDigit() }) {
            etMatriculaUsuario.error = "A matrícula deve conter apenas números"
            etMatriculaUsuario.requestFocus()
            return
        }

        // 🔎 VERIFICAR NO FIRESTORE SE EXISTE OUTRO USUÁRIO COM O MESMO EMAIL
        fb.collection(COLLECTION_USERS)
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snapEmail ->
                if (!snapEmail.isEmpty) {
                    etEmailUsuario.error = "Este e-mail já está cadastrado"
                    etEmailUsuario.requestFocus()
                    return@addOnSuccessListener
                }

                // 🔎 VERIFICAR MATRÍCULA DUPLICADA
                fb.collection(COLLECTION_USERS)
                    .whereEqualTo("matricula", matricula)
                    .get()
                    .addOnSuccessListener { snapMatricula ->
                        if (!snapMatricula.isEmpty) {
                            etMatriculaUsuario.error = "Esta matrícula já está cadastrada"
                            etMatriculaUsuario.requestFocus()
                            return@addOnSuccessListener
                        }

                        // Se passou por todas validações → confirma a operação
                        val mensagemConfirmacao = if (isAdmin) {
                            "Você está criando um novo ADMIN. Deseja continuar?"
                        } else {
                            "Deseja adicionar este usuário?"
                        }

                        AlertDialog.Builder(this)
                            .setTitle("Confirmar")
                            .setMessage(mensagemConfirmacao)
                            .setPositiveButton("Confirmar") { _, _ ->
                                salvarUsuario(nome, email, matricula, isAdmin)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao validar matrícula.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao validar e-mail.", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------- SALVAR NO FIRESTORE ----------------

    private fun salvarUsuario(
        nome: String,
        email: String,
        matricula: String,
        isAdmin: Boolean
    ) {
        // desabilita o botão pra evitar múltiplos cliques
        buttonSalvar.isEnabled = false
        buttonSalvar.text = "Salvando..."

        val dados = hashMapOf(
            "nomeCompleto" to nome,
            "email" to email,
            "matricula" to matricula,
            "isAdmin" to isAdmin,
            "ativo" to true,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now(),
            "senha" to "123456" // senha padrão - para mudar depois
        )

        fb.collection(COLLECTION_USERS)
            .add(dados)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Usuário adicionado com sucesso.",
                    Toast.LENGTH_LONG
                ).show()
                finish() // volta para ActivityAdminUsuarios
            }
            .addOnFailureListener { e ->
                buttonSalvar.isEnabled = true
                buttonSalvar.text = "Adicionar"
                Toast.makeText(
                    this,
                    "Erro ao adicionar usuário: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}