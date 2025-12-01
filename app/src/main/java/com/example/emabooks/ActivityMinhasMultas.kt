package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil

class ActivityMinhasMultas : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_MULTAS = "multas"
    private val COLLECTION_LIVROS = "livros"


    // Views
    private lateinit var toolbarMinhasMultas: MaterialToolbar
    private lateinit var tvResumoMultas: TextView
    private lateinit var tvComoRegularizar: TextView
    private lateinit var spinnerFiltroStatus: Spinner
    private lateinit var spinnerOrdenacaoData: Spinner
    private lateinit var rvMinhasMultas: RecyclerView
    private lateinit var tvMinhasMultasVazia: TextView
    private lateinit var btnPaginaAnterior: MaterialButton
    private lateinit var btnProximaPagina: MaterialButton
    private lateinit var tvPaginaInfo: TextView

    private lateinit var adapter: MultaUsuarioAdapter

    private val multasUsuario = mutableListOf<Multa>()
    private val livrosCache = mutableMapOf<String, String>() // livroId -> titulo

    // Paginação
    private val pageSize = 20
    private var currentPage = 0
    private var listaFiltrada: List<Multa> = emptyList()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minhas_multas)

        fb = Firebase.firestore

        bindViews()
        setupToolbar()
        setupRecycler()
        setupSpinners()
        setupListeners()

        carregarMultasDoUsuario()
    }

    /**
     * Obtém o ID do usuário logado usando o SessionManager.
     * O ID deve ser salvo no momento do login via SessionManager.salvarUsuarioId(context, userId).
     */
    private fun getUsuarioLogadoId(): String? {
        return SessionManager.obterUsuarioId(this)
    }

    private fun bindViews() {
        toolbarMinhasMultas = findViewById(R.id.toolbarMinhasMultas)
        tvResumoMultas = findViewById(R.id.tvResumoMultas)
        tvComoRegularizar = findViewById(R.id.tvComoRegularizar)
        spinnerFiltroStatus = findViewById(R.id.spinnerFiltroStatus)
        spinnerOrdenacaoData = findViewById(R.id.spinnerOrdenacaoData)
        rvMinhasMultas = findViewById(R.id.rvMinhasMultas)
        tvMinhasMultasVazia = findViewById(R.id.tvMinhasMultasVazia)
        btnPaginaAnterior = findViewById(R.id.btnPaginaAnterior)
        btnProximaPagina = findViewById(R.id.btnProximaPagina)
        tvPaginaInfo = findViewById(R.id.tvPaginaInfo)
    }

    private fun setupToolbar() {
        toolbarMinhasMultas.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecycler() {
        adapter = MultaUsuarioAdapter(emptyList()) { multa, tituloLivro ->
            // Se quiser no futuro abrir detalhes da multa para o usuário, pode usar esse clique.
            // Por enquanto, não faz nada (apenas card informativo).
        }
        rvMinhasMultas.layoutManager = LinearLayoutManager(this)
        rvMinhasMultas.adapter = adapter
    }

    private fun setupSpinners() {
        // Já usamos os arrays do XML, então só precisamos dos listeners
    }

    private fun setupListeners() {
        tvComoRegularizar.setOnClickListener {
            val intent = Intent(this, ActivityComoRegularizarMultas::class.java)
            startActivity(intent)
        }

        spinnerFiltroStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                aplicarFiltrosEAtualizarLista()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        spinnerOrdenacaoData.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                aplicarFiltrosEAtualizarLista()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        btnPaginaAnterior.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                atualizarPagina()
            }
        }

        btnProximaPagina.setOnClickListener {
            val totalPages = calcularTotalPaginas()
            if (currentPage < totalPages - 1) {
                currentPage++
                atualizarPagina()
            }
        }
    }

    private fun carregarMultasDoUsuario() {
        val userId = getUsuarioLogadoId()
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fb.collection(COLLECTION_MULTAS)
            .whereEqualTo("usuarioId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                multasUsuario.clear()

                for (doc in snapshot.documents) {
                    val multa = doc.toObject(Multa::class.java) ?: continue
                    multa.id = doc.id
                    multasUsuario.add(multa)
                }

                // 2) Carrega títulos dos livros para mapear no card
                carregarLivros()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar multas.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarLivros() {
        livrosCache.clear()

        fb.collection(COLLECTION_LIVROS)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val titulo = doc.getString("titulo") ?: doc.id
                    livrosCache[doc.id] = titulo
                }

                aplicarFiltrosEAtualizarLista()
            }
            .addOnFailureListener {
                aplicarFiltrosEAtualizarLista()
            }
    }

    private fun aplicarFiltrosEAtualizarLista() {
        // Filtro por status
        val statusSelecionado = spinnerFiltroStatus.selectedItem?.toString() ?: "Todos"
        val ordenacaoSelecionada = spinnerOrdenacaoData.selectedItem?.toString() ?: "Mais recentes"

        var lista = multasUsuario.toList()

        lista = when (statusSelecionado.lowercase(Locale.getDefault())) {
            "pendentes" -> lista.filter { it.status.equals("PENDENTE", ignoreCase = true) }
            "pagas" -> lista.filter { it.status.equals("PAGA", ignoreCase = true) }
            else -> lista
        }

        lista = when (ordenacaoSelecionada.lowercase(Locale.getDefault())) {
            "mais antigas" -> lista.sortedBy { it.dataMulta?.toDate() ?: java.util.Date(0) }
            else -> lista.sortedByDescending { it.dataMulta?.toDate() ?: java.util.Date(0) }
        }

        listaFiltrada = lista
        currentPage = 0

        atualizarResumo()
        atualizarPagina()
    }

    private fun atualizarResumo() {
        val totalMultas = multasUsuario.size
        val totalPendentes = multasUsuario
            .filter { it.status.equals("PENDENTE", ignoreCase = true) }
            .sumOf { it.valor }

        val textoResumo = "$totalMultas multas • ${currencyFormat.format(totalPendentes)} pendentes"
        tvResumoMultas.text = textoResumo
    }

    private fun calcularTotalPaginas(): Int {
        if (listaFiltrada.isEmpty()) return 1
        return ceil(listaFiltrada.size / pageSize.toDouble()).toInt()
    }

    private fun atualizarPagina() {
        if (listaFiltrada.isEmpty()) {
            rvMinhasMultas.visibility = View.GONE
            tvMinhasMultasVazia.visibility = View.VISIBLE
            btnPaginaAnterior.isEnabled = false
            btnProximaPagina.isEnabled = false
            tvPaginaInfo.text = "Página 0 de 0"
            return
        }

        rvMinhasMultas.visibility = View.VISIBLE
        tvMinhasMultasVazia.visibility = View.GONE

        val totalPages = calcularTotalPaginas()
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1
        }

        val fromIndex = currentPage * pageSize
        val toIndex = kotlin.math.min(fromIndex + pageSize, listaFiltrada.size)
        val pageItems = listaFiltrada.subList(fromIndex, toIndex)

        adapter.updateData(pageItems, livrosCache)

        btnPaginaAnterior.isEnabled = currentPage > 0
        btnProximaPagina.isEnabled = currentPage < totalPages - 1

        tvPaginaInfo.text = "Página ${currentPage + 1} de $totalPages"
    }

    // -----------------------------
    // Adapter da lista de multas do usuário
    // -----------------------------
    class MultaUsuarioAdapter(
        private var multas: List<Multa>,
        private val onClick: (Multa, String?) -> Unit
    ) : RecyclerView.Adapter<MultaUsuarioAdapter.MultaUsuarioViewHolder>() {

        private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        private var livrosCache: Map<String, String> = emptyMap()

        inner class MultaUsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDescricaoResumida: TextView =
                itemView.findViewById(R.id.tvMultaUsuarioDescricaoResumida)
            val tvValor: TextView = itemView.findViewById(R.id.tvMultaUsuarioValor)
            val tvData: TextView = itemView.findViewById(R.id.tvMultaUsuarioData)
            val tvStatus: TextView = itemView.findViewById(R.id.tvMultaUsuarioStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultaUsuarioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_multa_usuario, parent, false)
            return MultaUsuarioViewHolder(view)
        }

        override fun onBindViewHolder(holder: MultaUsuarioViewHolder, position: Int) {
            val multa = multas[position]

            val tituloLivro = multa.livroId?.let { livrosCache[it] } ?: ""
            val descricaoBase = multa.descricao.ifBlank { "Multa" }

            val descricaoResumida = if (tituloLivro.isNotBlank()) {
                "$tituloLivro - $descricaoBase"
            } else {
                descricaoBase
            }

            holder.tvDescricaoResumida.text = descricaoResumida
            holder.tvValor.text = currencyFormat.format(multa.valor)

            val data = multa.dataMulta?.toDate()
            holder.tvData.text = data?.let { sdf.format(it) } ?: "-"

            val statusLabel = if (multa.status.equals("PAGA", ignoreCase = true)) {
                "Paga"
            } else {
                "Pendente"
            }
            holder.tvStatus.text = statusLabel

            // Opcional: diferente cor para paga
            // (se quiser, pode alterar aqui com setTextColor baseado no status)

            holder.itemView.setOnClickListener {
                onClick(multa, tituloLivro)
            }
        }

        override fun getItemCount(): Int = multas.size

        fun updateData(novaLista: List<Multa>, livrosCache: Map<String, String>) {
            this.multas = novaLista
            this.livrosCache = livrosCache
            notifyDataSetChanged()
        }
    }
}