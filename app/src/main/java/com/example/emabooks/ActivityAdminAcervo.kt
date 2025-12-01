package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityAdminAcervo : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_LIVROS = "livros"

    // Views
    private lateinit var toolbarAcervoAdmin: MaterialToolbar
    private lateinit var etBuscaAcervo: EditText
    private lateinit var textViewQtdLivros: TextView
    private lateinit var rvLivrosAdmin: RecyclerView
    private lateinit var buttonAdicionarLivro: MaterialButton
    private lateinit var buttonPaginaAnterior: MaterialButton
    private lateinit var buttonPaginaProxima: MaterialButton
    private lateinit var textPaginaInfo: TextView

    // Lista em memória
    private val listaCompleta = mutableListOf<Livro>()
    private val listaFiltrada = mutableListOf<Livro>()
    private lateinit var livrosAdapter: LivrosAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_acervo)

        fb = Firebase.firestore

        bindViews()
        setupToolbar()
        setupRecycler()
        setupSearch()
        setupButtons()

        carregarLivros()
    }

    override fun onResume() {
        super.onResume()
        carregarLivros()
    }

    private fun bindViews() {
        toolbarAcervoAdmin = findViewById(R.id.toolbarAcervoAdmin)
        etBuscaAcervo = findViewById(R.id.etBuscaAcervo)
        textViewQtdLivros = findViewById(R.id.textViewQtdLivros)
        rvLivrosAdmin = findViewById(R.id.rvLivrosAdmin)
        buttonAdicionarLivro = findViewById(R.id.buttonAdicionarLivro)
        buttonPaginaAnterior = findViewById(R.id.buttonPaginaAnterior)
        buttonPaginaProxima = findViewById(R.id.buttonPaginaProxima)
        textPaginaInfo = findViewById(R.id.textPaginaInfo)

        buttonPaginaAnterior.isEnabled = false
        buttonPaginaProxima.isEnabled = false
        textPaginaInfo.text = "Página 1"
    }

    private fun setupToolbar() {
        toolbarAcervoAdmin.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecycler() {
        livrosAdapter = LivrosAdminAdapter(
            itens = listaFiltrada,
            onItemClick = { livro ->
                val intent = Intent(this, ActivityAdminLivroDetalhe::class.java)
                intent.putExtra(ActivityAdminLivroDetalhe.EXTRA_LIVRO_ID, livro.id)
                startActivity(intent)
            }
        )

        rvLivrosAdmin.apply {
            layoutManager = LinearLayoutManager(this@ActivityAdminAcervo)
            adapter = livrosAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSearch() {
        etBuscaAcervo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                aplicarFiltro(s?.toString().orEmpty())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupButtons() {
        buttonAdicionarLivro.setOnClickListener {
            val intent = Intent(this, ActivityAdminLivroForm::class.java)
            startActivity(intent)
        }

        buttonPaginaAnterior.setOnClickListener { /* paginação futura */ }
        buttonPaginaProxima.setOnClickListener { /* paginação futura */ }
    }

    private fun carregarLivros() {
        fb.collection(COLLECTION_LIVROS)
            .get()
            .addOnSuccessListener { snapshot ->
                listaCompleta.clear()

                for (doc in snapshot.documents) {
                    val livro = doc.toObject(Livro::class.java)
                    if (livro != null) {
                        listaCompleta.add(livro.copy(id = doc.id))
                    }
                }

                aplicarFiltro(etBuscaAcervo.text?.toString().orEmpty())
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao carregar acervo: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun aplicarFiltro(query: String) {
        val q = query.trim().lowercase()

        listaFiltrada.clear()

        if (q.isEmpty()) {
            listaFiltrada.addAll(listaCompleta)
        } else {
            listaFiltrada.addAll(
                listaCompleta.filter { livro ->
                    val titulo = livro.titulo ?: ""
                    val autor = livro.autor ?: ""
                    titulo.lowercase().contains(q) ||
                            autor.lowercase().contains(q)
                }
            )
        }

        livrosAdapter.notifyDataSetChanged()
        atualizarResumo()
    }

    private fun atualizarResumo() {
        val total = listaFiltrada.size
        textViewQtdLivros.text = "$total livro(s) encontrado(s)"
    }
}

/**
 * Adapter da listagem de livros (admin) – no mesmo arquivo.
 */
class LivrosAdminAdapter(
    private val itens: List<Livro>,
    private val onItemClick: (Livro) -> Unit
) : RecyclerView.Adapter<LivrosAdminAdapter.LivroAdminViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LivroAdminViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_livro, parent, false)
        return LivroAdminViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: LivroAdminViewHolder, position: Int) {
        holder.bind(itens[position])
    }

    override fun getItemCount(): Int = itens.size

    class LivroAdminViewHolder(
        itemView: View,
        private val onItemClick: (Livro) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val textLivroTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        private val textLivroAutor: TextView = itemView.findViewById(R.id.tvAutor)
        private val chipTipoLivro: TextView = itemView.findViewById(R.id.tvAno)
        private val chipStatusLivro: TextView = itemView.findViewById(R.id.tvStatus)

        private var currentLivro: Livro? = null

        init {
            itemView.setOnClickListener {
                currentLivro?.let { onItemClick(it) }
            }
        }

        fun bind(livro: Livro) {
            currentLivro = livro

            textLivroTitulo.text = livro.titulo
            textLivroAutor.text = livro.autor ?: "Autor não informado"

            // Tipo (Físico / Digital / Híbrido)
            val tipoText = when (livro.tipoAcervo) {
                TipoAcervo.DIGITAL -> "Digital"
                TipoAcervo.HIBRIDO -> "Híbrido"
                else -> "Físico"
            }
            chipTipoLivro.text = tipoText

            // Status (Disponível / Indisponível)
            val disponivel = livro.statusGeral == StatusLivro.DISPONIVEL
            chipStatusLivro.text = if (disponivel) "Disponível" else "Indisponível"
            chipStatusLivro.setBackgroundColor(
                if (disponivel) 0xFFDCFCE7.toInt() else 0xFFFEE2E2.toInt()
            )
            chipStatusLivro.setTextColor(
                if (disponivel) 0xFF166534.toInt() else 0xFFB91C1C.toInt()
            )
        }
    }
}