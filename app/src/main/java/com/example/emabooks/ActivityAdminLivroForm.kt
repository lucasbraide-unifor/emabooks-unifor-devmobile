package com.example.emabooks

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityAdminLivroForm : AppCompatActivity() {

    companion object {
        const val EXTRA_LIVRO_ID = "EXTRA_LIVRO_ID"
    }

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_LIVROS = "livros"

    // Views
    private lateinit var toolbarLivroForm: MaterialToolbar
    private lateinit var etTituloLivro: EditText
    private lateinit var etAutorLivro: EditText
    private lateinit var etAnoLivro: EditText
    private lateinit var spinnerTipoLivro: Spinner
    private lateinit var spinnerStatusLivro: Spinner
    private lateinit var etDescricaoLivro: EditText
    private lateinit var etLinkEbook: EditText
    private lateinit var tvErroLivroForm: TextView
    private lateinit var buttonCancelarLivro: MaterialButton
    private lateinit var buttonSalvarLivro: MaterialButton

    // Estado
    private var livroId: String? = null
    private var livroAtual: Livro? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_livro_form)

        fb = Firebase.firestore
        livroId = intent.getStringExtra(EXTRA_LIVRO_ID)

        bindViews()
        setupToolbar()
        setupSpinners()
        setupButtons()

        if (livroId != null) {
            toolbarLivroForm.title = "Editar livro"
            carregarLivroParaEdicao()
        } else {
            toolbarLivroForm.title = "Adicionar livro"
        }
    }

    private fun bindViews() {
        toolbarLivroForm = findViewById(R.id.toolbarLivroForm)
        etTituloLivro = findViewById(R.id.etTituloLivro)
        etAutorLivro = findViewById(R.id.etAutorLivro)
        etAnoLivro = findViewById(R.id.etAnoLivro)
        spinnerTipoLivro = findViewById(R.id.spinnerTipoLivro)
        spinnerStatusLivro = findViewById(R.id.spinnerStatusLivro)
        etDescricaoLivro = findViewById(R.id.etDescricaoLivro)
        etLinkEbook = findViewById(R.id.etLinkEbook)
        tvErroLivroForm = findViewById(R.id.tvErroLivroForm)
        buttonCancelarLivro = findViewById(R.id.buttonCancelarLivro)
        buttonSalvarLivro = findViewById(R.id.buttonSalvarLivro)
    }

    private fun setupToolbar() {
        toolbarLivroForm.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSpinners() {
        val tipos = arrayOf("Físico", "Digital", "Híbrido")
        spinnerTipoLivro.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tipos
        )

        val status = arrayOf("Disponível", "Indisponível")
        spinnerStatusLivro.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            status
        )
    }

    private fun setupButtons() {
        buttonCancelarLivro.setOnClickListener { finish() }
        buttonSalvarLivro.setOnClickListener { validarESalvar() }
    }

    // ---------------- CARREGAR PARA EDIÇÃO ----------------

    private fun carregarLivroParaEdicao() {
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
                preencherCamposEdicao(livroAtual!!)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar livro.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun preencherCamposEdicao(livro: Livro) {
        etTituloLivro.setText(livro.titulo)
        etAutorLivro.setText(livro.autor ?: "")
        etAnoLivro.setText(livro.anoPublicacao?.toString() ?: "")
        etDescricaoLivro.setText(livro.descricao ?: "")
        etLinkEbook.setText(livro.urlAcessoDigital ?: "")

        val tipoIndex = when (livro.tipoAcervo) {
            TipoAcervo.FISICO -> 0
            TipoAcervo.DIGITAL -> 1
            TipoAcervo.HIBRIDO -> 2
        }
        spinnerTipoLivro.setSelection(tipoIndex)

        val statusIndex = when (livro.statusGeral) {
            StatusLivro.DISPONIVEL -> 0
            StatusLivro.INDISPONIVEL -> 1
            StatusLivro.RESERVADO -> TODO()
            StatusLivro.ESGOTADO -> TODO()
            StatusLivro.PENDENTE -> TODO()
        }
        spinnerStatusLivro.setSelection(statusIndex)
    }

    // ---------------- VALIDAÇÃO ----------------

    private fun validarESalvar() {
        tvErroLivroForm.visibility = View.GONE

        val titulo = etTituloLivro.text.toString().trim()
        val autor = etAutorLivro.text.toString().trim()
        val anoStr = etAnoLivro.text.toString().trim()
        val descricao = etDescricaoLivro.text.toString().trim()
        val linkEbook = etLinkEbook.text.toString().trim()

        if (titulo.isEmpty()) {
            showError("Informe o título do livro.")
            etTituloLivro.requestFocus()
            return
        }

        if (autor.isEmpty()) {
            showError("Informe o autor do livro.")
            etAutorLivro.requestFocus()
            return
        }

        val tipo = when (spinnerTipoLivro.selectedItemPosition) {
            1 -> TipoAcervo.DIGITAL
            2 -> TipoAcervo.HIBRIDO
            else -> TipoAcervo.FISICO
        }

        val status = when (spinnerStatusLivro.selectedItemPosition) {
            1 -> StatusLivro.INDISPONIVEL
            else -> StatusLivro.DISPONIVEL
        }

        val anoPublicacao: Int? = if (anoStr.isNotEmpty()) {
            try {
                anoStr.toInt()
            } catch (e: NumberFormatException) {
                showError("Ano inválido.")
                etAnoLivro.requestFocus()
                return
            }
        } else null

        // Regras RF12.1 / RF13.1 – Digital precisa de link válido
        if (tipo == TipoAcervo.DIGITAL || tipo == TipoAcervo.HIBRIDO) {
            if (linkEbook.isEmpty()) {
                showError("Para livros digitais, informe o link do e-book.")
                etLinkEbook.requestFocus()
                return
            }
            if (!Patterns.WEB_URL.matcher(linkEbook).matches()) {
                showError("Informe um link de e-book válido (URL).")
                etLinkEbook.requestFocus()
                return
            }
        }

        salvarLivro(
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
        tvErroLivroForm.text = msg
        tvErroLivroForm.visibility = View.VISIBLE
    }

    // ---------------- SALVAR NO FIRESTORE ----------------

    private fun salvarLivro(
        titulo: String,
        autor: String,
        anoPublicacao: Int?,
        tipo: TipoAcervo,
        status: StatusLivro,
        descricao: String?,
        linkEbook: String?
    ) {
        // trava botão e guarda texto original
        val textoOriginal = buttonSalvarLivro.text.toString()
        buttonSalvarLivro.isEnabled = false
        buttonSalvarLivro.text = "Salvando..."

        // IMPORTANTE: os nomes das chaves devem bater com o Livro.kt
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

        val task = if (livroId == null) {
            dados["createdAt"] = Timestamp.now()
            fb.collection(COLLECTION_LIVROS).add(dados)
        } else {
            fb.collection(COLLECTION_LIVROS).document(livroId!!).set(dados)
        }

        task.addOnCompleteListener { resultado ->
            // SEMPRE restaurar o botão, deu certo ou não
            buttonSalvarLivro.isEnabled = true
            buttonSalvarLivro.text = textoOriginal

            if (resultado.isSuccessful) {
                val msg = if (livroId == null) {
                    "Livro cadastrado com sucesso."
                } else {
                    "Livro atualizado com sucesso."
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish() // agora garante que sai da tela
            } else {
                Toast.makeText(
                    this,
                    "Erro ao salvar livro: ${resultado.exception?.message ?: "tente novamente."}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}