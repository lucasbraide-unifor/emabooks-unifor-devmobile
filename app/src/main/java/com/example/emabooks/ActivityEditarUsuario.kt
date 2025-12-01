package com.example.emabooks

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityEditarUsuario : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_USERS = "users"

    // Views
    private lateinit var toolbarEditarUsuario: MaterialToolbar
    private lateinit var etNomeUsuario: EditText
    private lateinit var etEmailUsuario: EditText
    private lateinit var etMatriculaUsuario: EditText
    private lateinit var switchAdmin: Switch
    private lateinit var switchAtivo: Switch
    private lateinit var buttonSalvar: MaterialButton
    private lateinit var buttonCancelar: MaterialButton

    private var usuarioId: String? = null
    private var usuarioAtual: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_usuario)

        fb = Firebase.firestore
        usuarioId = intent.getStringExtra(ActivityAdminUsuarioDetalhe.EXTRA_USUARIO_ID)

        if (usuarioId.isNullOrBlank()) {
            Toast.makeText(this, "Usuário inválido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bindViews()
        setupToolbar()
        setupButtons()
        carregarUsuario()
    }

    private fun bindViews() {
        toolbarEditarUsuario = findViewById(R.id.toolbarEditarUsuario)
        etNomeUsuario = findViewById(R.id.etNomeUsuario)
        etEmailUsuario = findViewById(R.id.etEmailUsuario)
        etMatriculaUsuario = findViewById(R.id.etMatriculaUsuario)
        switchAdmin = findViewById(R.id.switchAdmin)
        switchAtivo = findViewById(R.id.switchAtivoUsuario)
        buttonSalvar = findViewById(R.id.buttonSalvar)
        buttonCancelar = findViewById(R.id.buttonCancelar)
    }

    private fun setupToolbar() {
        toolbarEditarUsuario.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButtons() {
        buttonCancelar.setOnClickListener {
            finish()
        }

        buttonSalvar.setOnClickListener {
            validarEAtualizar()
        }
    }

    // ---------------- CARREGAR DADOS ----------------

    private fun carregarUsuario() {
        val id = usuarioId ?: return

        fb.collection(COLLECTION_USERS)
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val usuario = doc.toObject(Usuario::class.java)
                if (usuario == null) {
                    Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                usuarioAtual = usuario.copy(id = doc.id)
                preencherCampos(usuarioAtual!!)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar usuário.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun preencherCampos(usuario: Usuario) {
        etNomeUsuario.setText(usuario.nomeCompleto ?: "")
        etEmailUsuario.setText(usuario.email ?: "")
        etMatriculaUsuario.setText(usuario.matricula ?: "")

        val isAdmin = usuario.isAdmin == true
        val isAtivo = usuario.ativo != false

        switchAdmin.isChecked = isAdmin
        switchAtivo.isChecked = isAtivo
    }

    // ---------------- VALIDAÇÃO + UPDATE ----------------

    private fun validarEAtualizar() {
        val nome = etNomeUsuario.text.toString().trim()
        val email = etEmailUsuario.text.toString().trim()
        val matricula = etMatriculaUsuario.text.toString().trim()
        val isAdmin = switchAdmin.isChecked
        val isAtivo = switchAtivo.isChecked

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

        if (matricula.isEmpty() or (matricula.length < 7)) {
            etMatriculaUsuario.error = "Informe a matrícula"
            etMatriculaUsuario.requestFocus()
            return
        }

        if (!matricula.all { it.isDigit() }) {
            etMatriculaUsuario.error = "A matrícula deve conter apenas números"
            etMatriculaUsuario.requestFocus()
            return
        }

        buttonSalvar.isEnabled = false
        buttonSalvar.text = "Salvando..."

        verificarDuplicidadeEmail(nome, email, matricula, isAdmin, isAtivo)
    }

    private fun verificarDuplicidadeEmail(
        nome: String,
        email: String,
        matricula: String,
        isAdmin: Boolean,
        isAtivo: Boolean
    ) {
        val idAtual = usuarioId ?: return

        fb.collection(COLLECTION_USERS)
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snap ->
                val existeOutro = snap.documents.any { it.id != idAtual }
                if (existeOutro) {
                    buttonSalvar.isEnabled = true
                    buttonSalvar.text = "Salvar"
                    etEmailUsuario.error = "Este e-mail já está cadastrado em outro usuário"
                    etEmailUsuario.requestFocus()
                    return@addOnSuccessListener
                }

                verificarDuplicidadeMatricula(nome, email, matricula, isAdmin, isAtivo)
            }
            .addOnFailureListener {
                buttonSalvar.isEnabled = true
                buttonSalvar.text = "Salvar"
                Toast.makeText(this, "Erro ao validar e-mail.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verificarDuplicidadeMatricula(
        nome: String,
        email: String,
        matricula: String,
        isAdmin: Boolean,
        isAtivo: Boolean
    ) {
        val idAtual = usuarioId ?: return

        fb.collection(COLLECTION_USERS)
            .whereEqualTo("matricula", matricula)
            .get()
            .addOnSuccessListener { snap ->
                val existeOutro = snap.documents.any { it.id != idAtual }
                if (existeOutro) {
                    buttonSalvar.isEnabled = true
                    buttonSalvar.text = "Salvar"
                    etMatriculaUsuario.error = "Esta matrícula já está cadastrada em outro usuário"
                    etMatriculaUsuario.requestFocus()
                    return@addOnSuccessListener
                }

                atualizarUsuario(nome, email, matricula, isAdmin, isAtivo)
            }
            .addOnFailureListener {
                buttonSalvar.isEnabled = true
                buttonSalvar.text = "Salvar"
                Toast.makeText(this, "Erro ao validar matrícula.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun atualizarUsuario(
        nome: String,
        email: String,
        matricula: String,
        isAdmin: Boolean,
        isAtivo: Boolean
    ) {
        val id = usuarioId ?: return

        val updates = mapOf(
            "nomeCompleto" to nome,
            "email" to email,
            "matricula" to matricula,
            "isAdmin" to isAdmin,
            "ativo" to isAtivo,
            "updatedAt" to Timestamp.now()
        )

        fb.collection(COLLECTION_USERS)
            .document(id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuário atualizado com sucesso.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                buttonSalvar.isEnabled = true
                buttonSalvar.text = "Salvar"
                Toast.makeText(
                    this,
                    "Erro ao atualizar usuário: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}