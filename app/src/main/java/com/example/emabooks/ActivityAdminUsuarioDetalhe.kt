package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityAdminUsuarioDetalhe : AppCompatActivity() {

    companion object {
        const val EXTRA_USUARIO_ID = "EXTRA_USUARIO_ID"
    }

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_USERS = "users"

    // Views
    private lateinit var toolbarDetalhe: MaterialToolbar
    private lateinit var tvNomeValor: TextView
    private lateinit var tvEmailValor: TextView
    private lateinit var tvMatriculaValor: TextView
    private lateinit var tvPapelValor: TextView
    private lateinit var tvStatusValor: TextView
    private lateinit var tvCriadoEmValor: TextView
    private lateinit var tvAtualizadoEmValor: TextView
    private lateinit var buttonEditar: MaterialButton
    private lateinit var buttonExcluir: MaterialButton

    private var usuarioId: String? = null
    private var usuarioAtual: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_usuario_detalhe)

        fb = Firebase.firestore
        bindViews()

        usuarioId = intent.getStringExtra(EXTRA_USUARIO_ID)
        if (usuarioId.isNullOrBlank()) {
            Toast.makeText(this, "Usuário inválido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupButtons()
        carregarDadosUsuario()
    }

    override fun onResume() {
        super.onResume()
        carregarDadosUsuario()
    }

    private fun bindViews() {
        toolbarDetalhe = findViewById(R.id.toolbarAdminUsuarioDetalhe)
        tvNomeValor = findViewById(R.id.tvNomeValor)
        tvEmailValor = findViewById(R.id.tvEmailValor)
        tvMatriculaValor = findViewById(R.id.tvMatriculaValor)
        tvPapelValor = findViewById(R.id.tvPapelValor)
        tvStatusValor = findViewById(R.id.tvStatusValor)
        tvCriadoEmValor = findViewById(R.id.tvCriadoEmValor)
        tvAtualizadoEmValor = findViewById(R.id.tvAtualizadoEmValor)
        buttonEditar = findViewById(R.id.buttonEditarUsuario)
        buttonExcluir = findViewById(R.id.buttonExcluirUsuario)
    }

    private fun setupToolbar() {
        toolbarDetalhe.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButtons() {
        buttonEditar.setOnClickListener {
            usuarioId?.let { id ->
                val intent = Intent(this, ActivityEditarUsuario::class.java)
                intent.putExtra(EXTRA_USUARIO_ID, id)
                startActivity(intent)
            }
        }

        buttonExcluir.setOnClickListener {
            solicitarConfirmacaoExclusao()
        }
    }

    private fun carregarDadosUsuario() {
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
        tvNomeValor.text = usuario.nomeCompleto ?: "-"
        tvEmailValor.text = usuario.email ?: "-"
        tvMatriculaValor.text = usuario.matricula ?: "-"

        val papel = if (usuario.isAdmin == true) "Admin" else "Usuário"
        tvPapelValor.text = papel

        val status = if (usuario.ativo != false) "Ativo" else "Inativo"
        tvStatusValor.text = status

        tvCriadoEmValor.text = formatarTimestamp(usuario.createdAt)
        tvAtualizadoEmValor.text = formatarTimestamp(usuario.updatedAt)
    }

    private fun formatarTimestamp(ts: Timestamp?): String {
        if (ts == null) return "-"
        val date = ts.toDate()
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(date)
    }

    private fun solicitarConfirmacaoExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Excluir usuário")
            .setMessage("Tem certeza que deseja excluir este usuário? Essa ação não poderá ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                excluirUsuario()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirUsuario() {
        val id = usuarioId ?: return

        buttonExcluir.isEnabled = false
        buttonEditar.isEnabled = false

        fb.collection(COLLECTION_USERS)
            .document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Usuário excluído com sucesso.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                buttonExcluir.isEnabled = true
                buttonEditar.isEnabled = true
                Toast.makeText(this, "Erro ao excluir usuário: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}