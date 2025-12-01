package com.example.emabooks

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityAdminLivroEditar : AppCompatActivity() {

    companion object {
        const val EXTRA_LIVRO_ID = "EXTRA_LIVRO_ID"
    }

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_LIVROS = "livros"

    // Views
    private lateinit var toolbarLivroEditar: MaterialToolbar
    private lateinit var etTituloLivroEditar: EditText
    private lateinit var etAutorLivroEditar: EditText
    private lateinit var etAnoLivroEditar: EditText
    private lateinit var spinnerTipoLivroEditar: Spinner
    private lateinit var spinnerStatusLivroEditar: Spinner
    private lateinit var etDescricaoLivroEditar: EditText
    private lateinit var etLinkEbookEditar: EditText
    private lateinit var tvErroLivroEditar: TextView
    private lateinit var buttonCancelarLivroEditar: MaterialButton
    private lateinit var buttonSalvarLivroEditar: MaterialButton

    // Estado
    private var livroId: String? = null
    private var livroAtual: Livro? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_livro_editar)

        fb = Firebase.firestore
        livroId = intent.getStringExtra(EXTRA_LIVRO_ID)

        if (livroId == null) {
            Toast.makeText(this, "Livro não informado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bindViews()
        setupToolbar()
        setupSpinners()
        setupButtons()
        carregarLivro()
    }

    private fun bindViews() {
        toolbarLivroEditar = findViewById(R.id.toolbarLivroEditar)
        etTituloLivroEditar = findViewById(R.id.etTituloLivroEditar)
        etAutorLivroEditar = findViewById(R.id.etAutorLivroEditar)
        etAnoLivroEditar = findViewById(R.id.etAnoLivroEditar)
        spinnerTipoLivroEditar = findViewById(R.id.spinnerTipoLivroEditar)
        spinnerStatusLivroEditar = findViewById(R.id.spinnerStatusLivroEditar)
        etDescricaoLivroEditar = findViewById(R.id.etDescricaoLivroEditar)
        etLinkEbookEditar = findViewById(R.id.etLinkEbookEditar)
        tvErroLivroEditar = findViewById(R.id.tvErroLivroEditar)
        buttonCancelarLivroEditar = findViewById(R.id.buttonCancelarLivroEditar)
        buttonSalvarLivroEditar = findViewById(R.id.buttonSalvarLivroEditar)
    }

    private fun setupToolbar() {
        toolbarLivroEditar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSpinners() {
        val tipos = arrayOf("Físico", "Digital", "Híbrido")
        spinnerTipoLivroEditar.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tipos
        )

        // Para simplificar, vamos manter só Disponível/Indisponível aqui
        val status = arrayOf("Disponível", "Indisponível")
        spinnerStatusLivroEditar.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            status
        )
    }

    private fun setupButtons() {
        buttonCancelarLivroEditar.setOnClickListener { finish() }
        buttonSalvarLivroEditar.setOnClickListener { validarESalvar() }
    }

    // ---------------- CARREGAR DADOS ----------------

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
        etTituloLivroEditar.setText(livro.titulo)
        etAutorLivroEditar.setText(livro.autor ?: "")
        etAnoLivroEditar.setText(livro.anoPublicacao?.toString() ?: "")
        etDescricaoLivroEditar.setText(livro.descricao ?: "")
        etLinkEbookEditar.setText(livro.urlAcessoDigital ?: "")

        // Tipo
        val tipoIndex = when (livro.tipoAcervo) {
            TipoAcervo.FISICO -> 0
            TipoAcervo.DIGITAL -> 1
            TipoAcervo.HIBRIDO -> 2
        }
        spinnerTipoLivroEditar.setSelection(tipoIndex)

        // Status -> mapeia qualquer coisa diferente de DISPONIVEL como "Indisponível"
        val statusIndex = when (livro.statusGeral) {
            StatusLivro.DISPONIVEL -> 0
            else -> 1
        }
        spinnerStatusLivroEditar.setSelection(statusIndex)
    }

    // ---------------- VALIDAÇÃO ----------------

    private fun validarESalvar() {
        tvErroLivroEditar.visibility = View.GONE

        val titulo = etTituloLivroEditar.text.toString().trim()
        val autor = etAutorLivroEditar.text.toString().trim()
        val anoStr = etAnoLivroEditar.text.toString().trim()
        val descricao = etDescricaoLivroEditar.text.toString().trim()
        val linkEbook = etLinkEbookEditar.text.toString().trim()

        if (titulo.isEmpty()) {
            showError("Informe o título do livro.")
            etTituloLivroEditar.requestFocus()
            return
        }

        if (autor.isEmpty()) {
            showError("Informe o autor do livro.")
            etAutorLivroEditar.requestFocus()
            return
        }

        val tipo = when (spinnerTipoLivroEditar.selectedItemPosition) {
            1 -> TipoAcervo.DIGITAL
            2 -> TipoAcervo.HIBRIDO
            else -> TipoAcervo.FISICO
        }

        val status = when (spinnerStatusLivroEditar.selectedItemPosition) {
            1 -> StatusLivro.INDISPONIVEL
            else -> StatusLivro.DISPONIVEL
        }

        val anoPublicacao: Int? = if (anoStr.isNotEmpty()) {
            try {
                anoStr.toInt()
            } catch (e: NumberFormatException) {
                showError("Ano inválido.")
                etAnoLivroEditar.requestFocus()
                return
            }
        } else null

        // Digital/Híbrido precisa de link válido
        if (tipo == TipoAcervo.DIGITAL || tipo == TipoAcervo.HIBRIDO) {
            if (linkEbook.isEmpty()) {
                showError("Para livros digitais, informe o link do e-book.")
                etLinkEbookEditar.requestFocus()
                return
            }
            if (!Patterns.WEB_URL.matcher(linkEbook).matches()) {
                showError("Informe um link de e-book válido (URL).")
                etLinkEbookEditar.requestFocus()
                return
            }
        }

        salvarAlteracoes(
            titulo = titulo,
            autor = autor,
            anoPublicacao = anoPublicacao,
            tipo = tipo,
            status = status,
            descricao = if (descricao.isEmpty()) null else descricao,
            linkEbook = if (linkEbook.isEmpty()) null else linkEbook
        )
    }

    private fun showError(msg: String) {
        tvErroLivroEditar.text = msg
        tvErroLivroEditar.visibility = View.VISIBLE
    }

    // ---------------- SALVAR NO FIRESTORE ----------------

    private fun salvarAlteracoes(
        titulo: String,
        autor: String,
        anoPublicacao: Int?,
        tipo: TipoAcervo,
        status: StatusLivro,
        descricao: String?,
        linkEbook: String?
    ) {
        val id = livroId ?: return

        val textoOriginal = buttonSalvarLivroEditar.text.toString()
        buttonSalvarLivroEditar.isEnabled = false
        buttonSalvarLivroEditar.text = "Salvando..."

        val dados = hashMapOf<String, Any?>(
            "titulo" to titulo,
            "autor" to autor,
            "anoPublicacao" to anoPublicacao,
            "tipoAcervo" to tipo.name,
            "statusGeral" to status.name,
            "descricao" to descricao,
            "urlAcessoDigital" to linkEbook,
            "updatedAt" to Timestamp.now()
        )

        fb.collection(COLLECTION_LIVROS)
            .document(id)
            .update(dados)
            .addOnCompleteListener { task ->
                buttonSalvarLivroEditar.isEnabled = true
                buttonSalvarLivroEditar.text = textoOriginal

                if (task.isSuccessful) {
                    Toast.makeText(this, "Livro atualizado com sucesso.", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Erro ao atualizar livro: ${task.exception?.message ?: "tente novamente."}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}