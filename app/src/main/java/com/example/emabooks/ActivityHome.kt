package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.util.Locale
import kotlin.math.ceil

class ActivityHome : AppCompatActivity() {

    // Views
    private lateinit var etBuscaLivro: EditText
    private lateinit var btnBuscarLivro: Button
    private lateinit var tvBuscando: TextView
    private lateinit var rvLivros: RecyclerView

    private lateinit var tvFiltroDisponiveis: TextView
    private lateinit var tvFiltroIndisponiveis: TextView
    private lateinit var tvOrdenarTitulo: TextView
    private lateinit var tvOrdenarAutor: TextView
    private lateinit var tvInfoPagina: TextView
    private lateinit var tvLimparFiltros: TextView

    private var tvPaginaAnterior: TextView? = null
    private var tvPaginaProxima: TextView? = null

    private lateinit var bottomNavigation: BottomNavigationView

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val livrosCollection = "livros"

    // Dados / estado
    private val pageSize = 5
    private var isSearching = false
    private var currentPage = 1
    private var totalPages = 1

    private var currentSearchTerm: String? = null

    private var availabilityFilter: AvailabilityFilter = AvailabilityFilter.ALL
    private var sortOption: SortOption = SortOption.TITLE

    private lateinit var adapter: BookAdapter

    // Catálogo vindo do Firestore
    private var allBooks: List<Livro> = emptyList()
    private var lastFilteredList: List<Livro> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        fb = FirebaseFirestore.getInstance()

        bindViews()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupOrdering()
        setupPaginationControls()
        setupBottomNavigation()

        performInitialLoad()
    }

    override fun onResume() {
        super.onResume()
        // Sempre que a Activity voltar para o foreground (inclusive depois da tela de detalhes),
        // recarrega os livros reais do Firestore respeitando filtros/paginação.
        performInitialLoad()
    }

    // ---------------- NAV BOTTOM ---------------- //

    private fun setupBottomNavigation() {
        // Marca o item de acervo como selecionado na tela Home
        bottomNavigation.selectedItemId = R.id.nav_acervo

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_acervo -> {
                    // Já está na tela de acervo
                    true
                }
                R.id.nav_emprestimos -> {
                    // Abre a tela de reservas/emprestimos
                    val intent = Intent(this, ActivityReservas::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_minha_conta -> {
                    val intent = Intent(this, ActivityMinhaConta::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun bindViews() {
        etBuscaLivro = findViewById(R.id.etBuscaLivro)
        btnBuscarLivro = findViewById(R.id.btnBuscarLivro)
        tvBuscando = findViewById(R.id.tvBuscando)
        rvLivros = findViewById(R.id.rvLivros)
        rvLivros.isNestedScrollingEnabled = false

        tvFiltroDisponiveis = findViewById(R.id.tvFiltroDisponiveis)
        tvFiltroIndisponiveis = findViewById(R.id.tvFiltroIndisponiveis)
        tvOrdenarTitulo = findViewById(R.id.tvOrdenarTitulo)
        tvOrdenarAutor = findViewById(R.id.tvOrdenarAutor)
        tvInfoPagina = findViewById(R.id.tvInfoPagina)
        tvLimparFiltros = findViewById(R.id.tvLimparFiltros)

        tvPaginaAnterior = findViewById(R.id.tvPaginaAnterior)
        tvPaginaProxima = findViewById(R.id.tvPaginaProxima)

        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter { livroClicado ->
            abrirDetalheLivro(livroClicado)
        }
        rvLivros.layoutManager = LinearLayoutManager(this)
        rvLivros.adapter = adapter
    }

    private fun abrirDetalheLivro(livro: Livro) {
        val intent = Intent(this, ActivityBookDetails::class.java)
        intent.putExtra(ActivityBookDetails.EXTRA_LIVRO, livro) // Livro é Serializable
        startActivity(intent)
    }

    // ------------ Busca ------------ //

    private fun setupSearch() {
        btnBuscarLivro.setOnClickListener {
            triggerSearch()
        }

        etBuscaLivro.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnter) {
                triggerSearch()
                true
            } else {
                false
            }
        }

        etBuscaLivro.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 100) {
                    etBuscaLivro.setText(s.substring(0, 100))
                    etBuscaLivro.setSelection(etBuscaLivro.text.length)
                    Toast.makeText(
                        this@ActivityHome,
                        "Limite de 100 caracteres atingido",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun triggerSearch() {
        if (isSearching) return

        val rawInput = etBuscaLivro.text?.toString() ?: ""
        val term = rawInput.trim()

        if (!validateSearchTerm(term)) return

        currentSearchTerm = term
        performSearch(term)
    }

    private fun validateSearchTerm(term: String): Boolean {
        if (term.isEmpty()) {
            Toast.makeText(this, "Digite ao menos 2 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        if (term.length < 2) {
            Toast.makeText(this, "Digite ao menos 2 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        if (term.length > 100) {
            Toast.makeText(this, "O termo não pode ultrapassar 100 caracteres", Toast.LENGTH_SHORT)
                .show()
            return false
        }

        return true
    }

    private fun performSearch(term: String) {
        try {
            isSearching = true
            setLoading(true)

            val normalizedTerm = normalize(term)

            val resultsWithFlags = allBooks.mapNotNull { livro ->
                val titleNorm = normalize(livro.titulo)
                val authorNorm = normalize(livro.autor ?: "")

                val matchesTitle = titleNorm.contains(normalizedTerm)
                val matchesAuthor = authorNorm.contains(normalizedTerm)

                if (matchesTitle || matchesAuthor) {
                    SearchResult(livro, matchesTitle, matchesAuthor)
                } else {
                    null
                }
            }

            val sortedResults = resultsWithFlags.sortedWith(
                compareByDescending<SearchResult> { it.livro.isDisponivelParaUI() }
                    .thenByDescending { it.matchesTitle }
                    .thenBy { sr ->
                        when (sortOption) {
                            SortOption.TITLE -> normalize(sr.livro.titulo)
                            SortOption.AUTHOR -> normalize(sr.livro.autor ?: "")
                        }
                    }
            ).map { it.livro }

            val filtered = applyAvailabilityFilter(sortedResults)

            updatePagedList(filtered)

        } catch (e: Exception) {
            handleSearchError()
        } finally {
            setLoading(false)
            isSearching = false
        }
    }

    private fun setLoading(isLoading: Boolean) {
        tvBuscando.visibility = if (isLoading) TextView.VISIBLE else TextView.GONE
        btnBuscarLivro.isEnabled = !isLoading
        etBuscaLivro.isEnabled = !isLoading
    }

    private fun normalize(text: String): String {
        val lower = text.lowercase(Locale.getDefault())
        val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    // ------------ Carregamento inicial (livros reais) ------------ //

    private fun performInitialLoad() {
        currentSearchTerm = null
        setLoading(true)

        fb.collection(livrosCollection)
            .get()
            .addOnSuccessListener { snap ->
                val lista = snap.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val titulo = doc.getString("titulo") ?: "Sem título"
                        val autor = doc.getString("autor")
                        val ano = doc.getLong("anoPublicacao")?.toInt()
                        val capaUrl = doc.getString("capaUrl")

                        val tipoStr = doc.getString("tipoAcervo") ?: "FISICO"
                        val tipoAcervo = try {
                            TipoAcervo.valueOf(tipoStr)
                        } catch (_: Exception) {
                            TipoAcervo.FISICO
                        }

                        val statusStr = doc.getString("statusGeral") ?: "DISPONIVEL"
                        val statusGeral = try {
                            StatusLivro.valueOf(statusStr)
                        } catch (_: Exception) {
                            StatusLivro.DISPONIVEL
                        }

                        val categorias = (doc.get("categorias") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList()

                        val totalFisicos =
                            doc.getLong("totalExemplaresFisicos")?.toInt() ?: 0
                        val exemplaresDisp =
                            doc.getLong("exemplaresFisicosDisponiveis")?.toInt() ?: 0

                        Livro(
                            id = id,
                            titulo = titulo,
                            autor = autor,
                            anoPublicacao = ano,
                            editora = doc.getString("editora"),
                            tipoAcervo = tipoAcervo,
                            categorias = categorias,
                            capaUrl = capaUrl,
                            statusGeral = statusGeral,
                            totalExemplaresFisicos = totalFisicos,
                            exemplaresFisicosDisponiveis = exemplaresDisp,
                            urlAcessoDigital = doc.getString("urlAcessoDigital"),
                            descricao = doc.getString("descricao"),
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                allBooks = lista.sortedWith(
                    compareByDescending<Livro> { it.isDisponivelParaUI() }
                        .thenBy { it.titulo.lowercase() }
                )

                // estado "limpo" inicial
                availabilityFilter = AvailabilityFilter.ALL
                sortOption = SortOption.TITLE
                currentSearchTerm = null
                updateFilterStyle()
                updateSortStyle()

                updatePagedList(allBooks)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar livros.", Toast.LENGTH_SHORT).show()
                updatePagedList(emptyList())
            }
            .addOnCompleteListener {
                setLoading(false)
            }
    }

    // ------------ Filtros / Ordenação ------------ //

    private fun setupFilters() {
        tvFiltroDisponiveis.setOnClickListener {
            availabilityFilter = AvailabilityFilter.AVAILABLE
            updateFilterStyle()
            refreshCurrentList()
        }

        tvFiltroIndisponiveis.setOnClickListener {
            availabilityFilter = AvailabilityFilter.UNAVAILABLE
            updateFilterStyle()
            refreshCurrentList()
        }

        // NOVO: limpar filtros e busca
        tvLimparFiltros.setOnClickListener {
            clearFiltersAndSearch()
        }
    }

    private fun setupOrdering() {
        tvOrdenarTitulo.setOnClickListener {
            sortOption = SortOption.TITLE
            updateSortStyle()
            refreshCurrentList()
        }

        tvOrdenarAutor.setOnClickListener {
            sortOption = SortOption.AUTHOR
            updateSortStyle()
            refreshCurrentList()
        }
    }

    private fun updateFilterStyle() {
        tvFiltroDisponiveis.setTypeface(
            null,
            if (availabilityFilter == AvailabilityFilter.AVAILABLE)
                android.graphics.Typeface.BOLD
            else
                android.graphics.Typeface.NORMAL
        )
        tvFiltroIndisponiveis.setTypeface(
            null,
            if (availabilityFilter == AvailabilityFilter.UNAVAILABLE)
                android.graphics.Typeface.BOLD
            else
                android.graphics.Typeface.NORMAL
        )
    }

    private fun updateSortStyle() {
        tvOrdenarTitulo.setTypeface(
            null,
            if (sortOption == SortOption.TITLE)
                android.graphics.Typeface.BOLD
            else
                android.graphics.Typeface.NORMAL
        )
        tvOrdenarAutor.setTypeface(
            null,
            if (sortOption == SortOption.AUTHOR)
                android.graphics.Typeface.BOLD
            else
                android.graphics.Typeface.NORMAL
        )
    }

    private fun applyAvailabilityFilter(list: List<Livro>): List<Livro> {
        return when (availabilityFilter) {
            AvailabilityFilter.ALL -> list
            AvailabilityFilter.AVAILABLE -> list.filter { it.isDisponivelParaUI() }
            AvailabilityFilter.UNAVAILABLE -> list.filter { !it.isDisponivelParaUI() }
        }
    }

    private fun refreshCurrentList() {
        val term = currentSearchTerm
        if (term.isNullOrBlank()) {
            // Reaplica filtros sobre a lista carregada em memória
            val base = when (sortOption) {
                SortOption.TITLE -> allBooks.sortedWith(
                    compareByDescending<Livro> { it.isDisponivelParaUI() }
                        .thenBy { it.titulo.lowercase() }
                )
                SortOption.AUTHOR -> allBooks.sortedWith(
                    compareByDescending<Livro> { it.isDisponivelParaUI() }
                        .thenBy { (it.autor ?: "").lowercase() }
                )
            }
            updatePagedList(applyAvailabilityFilter(base))
        } else {
            performSearch(term)
        }
    }

    // ------------ Limpar filtros e busca ------------ //

    private fun clearFiltersAndSearch() {
        // Reseta estado
        availabilityFilter = AvailabilityFilter.ALL
        sortOption = SortOption.TITLE
        currentSearchTerm = null

        // Limpa campo de busca
        etBuscaLivro.setText("")

        // Atualiza estilos visuais
        updateFilterStyle()
        updateSortStyle()

        // Reaproveita lista base já carregada em memória
        val base = allBooks.sortedWith(
            compareByDescending<Livro> { it.isDisponivelParaUI() }
                .thenBy { it.titulo.lowercase() }
        )

        updatePagedList(base)

        Toast.makeText(this, "Filtros e busca limpos.", Toast.LENGTH_SHORT).show()
    }

    // ------------ Paginação ------------ //

    private fun setupPaginationControls() {
        tvPaginaAnterior?.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePageItems()
            }
        }

        tvPaginaProxima?.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                updatePageItems()
            }
        }
    }

    private fun updatePagedList(filteredList: List<Livro>) {
        lastFilteredList = filteredList

        if (filteredList.isEmpty()) {
            adapter.submitList(emptyList())
            val term = currentSearchTerm
            tvInfoPagina.text = if (!term.isNullOrBlank()) {
                "Nenhum item encontrado para '$term'"
            } else {
                "Nenhum item encontrado"
            }
            totalPages = 0
            currentPage = 0
            updatePaginationVisibility()
            return
        }

        totalPages = ceil(filteredList.size / pageSize.toDouble()).toInt()
        currentPage = 1
        updatePageItems()
    }

    private fun updatePageItems() {
        if (lastFilteredList.isEmpty() || totalPages == 0) {
            adapter.submitList(emptyList())
            tvPaginaAnterior?.isEnabled = false
            tvPaginaProxima?.isEnabled = false
            return
        }

        val fromIndex = (currentPage - 1) * pageSize
        val toIndex = minOf(fromIndex + pageSize, lastFilteredList.size)
        val pageItems = lastFilteredList.subList(fromIndex, toIndex)

        adapter.submitList(pageItems)

        val totalItems = lastFilteredList.size
        tvInfoPagina.text = "Página $currentPage de $totalPages • $totalItems itens"

        updatePaginationVisibility()
    }

    private fun updatePaginationVisibility() {
        tvPaginaAnterior?.isEnabled = currentPage > 1
        tvPaginaProxima?.isEnabled = currentPage < totalPages
    }

    // ------------ Erros técnicos ------------ //

    private fun handleSearchError() {
        val term = currentSearchTerm ?: etBuscaLivro.text?.toString()?.trim().orEmpty()
        AlertDialog.Builder(this)
            .setTitle("Erro na busca")
            .setMessage("Não foi possível realizar a busca agora. Tente novamente.")
            .setPositiveButton("Tentar novamente") { _, _ ->
                if (term.isNotBlank()) {
                    performSearch(term)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ------------ Models / Adapter ------------ //

    enum class AvailabilityFilter {
        ALL, AVAILABLE, UNAVAILABLE
    }

    enum class SortOption {
        TITLE, AUTHOR
    }

    private data class SearchResult(
        val livro: Livro,
        val matchesTitle: Boolean,
        val matchesAuthor: Boolean
    )

    class BookAdapter(
        private val onBookClick: (Livro) -> Unit
    ) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

        private val items = mutableListOf<Livro>()

        fun submitList(list: List<Livro>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): BookViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_livro, parent, false)
            return BookViewHolder(view, onBookClick)
        }

        override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class BookViewHolder(
            itemView: android.view.View,
            private val onBookClick: (Livro) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {

            private val ivCapa: ImageView = itemView.findViewById(R.id.imgCapa)
            private val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
            private val tvAutor: TextView = itemView.findViewById(R.id.tvAutor)
            private val tvAno: TextView = itemView.findViewById(R.id.tvAno)
            private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

            private var currentLivro: Livro? = null

            init {
                itemView.setOnClickListener {
                    currentLivro?.let { onBookClick(it) }
                }
            }

            fun bind(livro: Livro) {
                currentLivro = livro

                tvTitulo.text = livro.titulo
                tvTitulo.contentDescription = "Título do livro: ${livro.titulo}"

                if (!livro.autor.isNullOrBlank()) {
                    tvAutor.text = livro.autor
                    tvAutor.visibility = TextView.VISIBLE
                } else {
                    tvAutor.text = ""
                    tvAutor.visibility = TextView.GONE
                }

                if (livro.anoPublicacao != null) {
                    tvAno.text = livro.anoPublicacao.toString()
                    tvAno.visibility = TextView.VISIBLE
                } else {
                    tvAno.text = ""
                    tvAno.visibility = TextView.GONE
                }

                val disponivel = livro.isDisponivelParaUI()
                tvStatus.text = if (disponivel) "Disponível" else "Indisponível"
                tvStatus.setTextColor(
                    if (disponivel)
                        itemView.context.getColor(android.R.color.holo_green_dark)
                    else
                        itemView.context.getColor(android.R.color.holo_red_dark)
                )

                if (livro.capaUrl.isNullOrBlank()) {
                    ivCapa.setImageResource(R.drawable.ic_book_placeholder)
                } else {
                    // Aqui no futuro você pode usar Glide/Picasso
                    ivCapa.setImageResource(R.drawable.ic_book_placeholder)
                }

                itemView.contentDescription =
                    "Livro ${livro.titulo}, autor ${livro.autor ?: "não informado"}, ano ${livro.anoPublicacao ?: 0}, status ${tvStatus.text}"
            }
        }
    }
}

/**
 * Regra centralizada de disponibilidade para a UI
 */
private fun Livro.isDisponivelParaUI(): Boolean {
    if (statusGeral != StatusLivro.DISPONIVEL) return false
    return when (tipoAcervo) {
        TipoAcervo.FISICO -> exemplaresFisicosDisponiveis > 0
        TipoAcervo.DIGITAL, TipoAcervo.HIBRIDO -> true
    }
}