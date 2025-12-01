package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.view.View   // <-- IMPORT IMPORTANTE
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityAdminLivroDetalhe : AppCompatActivity() {

    companion object {
        const val EXTRA_LIVRO_ID = "EXTRA_LIVRO_ID"
    }

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_LIVROS = "livros"
    private val COLLECTION_EMPRESTIMOS = "emprestimos"
    private val COLLECTION_RESERVAS = "reservas"

    // Views
    private lateinit var toolbarLivroDetalhe: MaterialToolbar
    private lateinit var textTituloLivroDetalhe: TextView
    private lateinit var textAutorLivroDetalhe: TextView
    private lateinit var textAnoLivroDetalhe: TextView
    private lateinit var textTipoLivroDetalhe: TextView
    private lateinit var textStatusLivroDetalhe: TextView
    private lateinit var textDescricaoLivroDetalhe: TextView
    private lateinit var textLinkEbookLabel: TextView
    private lateinit var textLinkEbookValor: TextView
    private lateinit var buttonEditarLivro: MaterialButton
    private lateinit var buttonExcluirLivro: MaterialButton

    private var livroId: String? = null
    private var livroAtual: Livro? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_livro_detalhe)

        fb = Firebase.firestore
        livroId = intent.getStringExtra(EXTRA_LIVRO_ID)

        if (livroId == null) {
            Toast.makeText(this, "Livro não informado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bindViews()
        setupToolbar()
        setupButtons()
        carregarLivro()
    }

    private fun bindViews() {
        toolbarLivroDetalhe = findViewById(R.id.toolbarLivroDetalhe)
        textTituloLivroDetalhe = findViewById(R.id.textTituloLivroDetalhe)
        textAutorLivroDetalhe = findViewById(R.id.textAutorLivroDetalhe)
        textAnoLivroDetalhe = findViewById(R.id.textAnoLivroDetalhe)
        textTipoLivroDetalhe = findViewById(R.id.textTipoLivroDetalhe)
        textStatusLivroDetalhe = findViewById(R.id.textStatusLivroDetalhe)
        textDescricaoLivroDetalhe = findViewById(R.id.textDescricaoLivroDetalhe)
        textLinkEbookLabel = findViewById(R.id.textLinkEbookLabel)
        textLinkEbookValor = findViewById(R.id.textLinkEbookValor)
        buttonEditarLivro = findViewById(R.id.buttonEditarLivro)
        buttonExcluirLivro = findViewById(R.id.buttonExcluirLivro)
    }

    private fun setupToolbar() {
        toolbarLivroDetalhe.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButtons() {
        buttonEditarLivro.setOnClickListener {
            val id = livroId ?: return@setOnClickListener
            val intent = Intent(this, ActivityAdminLivroEditar::class.java)
            intent.putExtra(ActivityAdminLivroEditar.EXTRA_LIVRO_ID, id)
            startActivity(intent)
        }

        buttonExcluirLivro.setOnClickListener {
            confirmarExclusao()
        }
    }

    private fun carregarLivro() {
        val id = livroId ?: return

        fb.collection(COLLECTION_LIVROS)
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                val livro = doc.toObject(Livro::class.java)
                if (livro == null) {
                    Toast.makeText(this, "Livro não encontrado.", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                livroAtual = livro.copy(id = doc.id)
                preencherCampos(livroAtual!!)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar livro.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun preencherCampos(livro: Livro) {
        textTituloLivroDetalhe.text = livro.titulo
        textAutorLivroDetalhe.text = livro.autor ?: "Autor não informado"
        textAnoLivroDetalhe.text = "Ano: ${livro.anoPublicacao ?: "—"}"

        val tipoText = when (livro.tipoAcervo) {
            TipoAcervo.DIGITAL -> "Digital"
            TipoAcervo.HIBRIDO -> "Híbrido"
            else -> "Físico"
        }
        textTipoLivroDetalhe.text = "Tipo: $tipoText"

        val disponivel = livro.statusGeral == StatusLivro.DISPONIVEL
        textStatusLivroDetalhe.text =
            "Status: " + if (disponivel) "Disponível" else "Indisponível"

        textDescricaoLivroDetalhe.text =
            if (livro.descricao.isNullOrBlank()) "Sem descrição cadastrada."
            else livro.descricao

        // --- LINK DO E-BOOK (pode comentar isso se ainda não tiver o campo no Livro) ---
        if (livro.tipoAcervo == TipoAcervo.DIGITAL || livro.tipoAcervo == TipoAcervo.HIBRIDO) {
            textLinkEbookLabel.visibility = View.VISIBLE
            textLinkEbookValor.visibility = View.VISIBLE

            val link = livro.urlAcessoDigital   // <- certifique-se de que existe em Livro.kt
            textLinkEbookValor.text =
                if (link.isNullOrBlank()) "Link não informado."
                else link
        } else {
            textLinkEbookLabel.visibility = View.GONE
            textLinkEbookValor.visibility = View.GONE
        }
    }

    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Excluir livro")
            .setMessage("Tem certeza que deseja excluir este livro?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                verificarDependenciasEExcluir()
            }
            .show()
    }

    private fun verificarDependenciasEExcluir() {
        val id = livroId ?: return

        val emprestimosTask = fb.collection(COLLECTION_EMPRESTIMOS)
            .whereEqualTo("livroId", id)
            .whereEqualTo("status", "ATIVO")
            .get()

        val reservasTask = fb.collection(COLLECTION_RESERVAS)
            .whereEqualTo("livroId", id)
            .whereEqualTo("status", "PENDENTE")
            .get()

        Tasks.whenAllSuccess<Any>(emprestimosTask, reservasTask)
            .addOnSuccessListener {
                val emprestimosAtivos = !emprestimosTask.result.isEmpty
                val reservasPendentes = !reservasTask.result.isEmpty

                if (emprestimosAtivos || reservasPendentes) {
                    Toast.makeText(
                        this,
                        "Não é possível excluir: há empréstimo ou reserva ativa para este livro.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                fb.collection(COLLECTION_LIVROS)
                    .document(id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Livro excluído com sucesso.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Erro ao excluir livro: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Erro ao validar dependências para exclusão.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}