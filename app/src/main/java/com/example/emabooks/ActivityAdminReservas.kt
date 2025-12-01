package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityAdminReservas : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_RESERVAS = "reservas"
    private val COLLECTION_USERS = "users"
    private val COLLECTION_LIVROS = "livros"

    // Cache de nomes de usuário e títulos de livros
    private val usuariosCache = mutableMapOf<String, String>()
    private val livrosCache = mutableMapOf<String, String>()

    // Views
    private lateinit var toolbarAdminReservas: MaterialToolbar
    private lateinit var etBuscaReserva: EditText
    private lateinit var btnBuscarReserva: MaterialButton
    private lateinit var switchFiltrarPendentes: SwitchMaterial
    private lateinit var rvReservas: RecyclerView
    private lateinit var tvListaReservasVazia: TextView
    private lateinit var fabNovaReserva: FloatingActionButton

    private lateinit var tvResumoPendentesValor: TextView
    private lateinit var tvResumoAprovadasValor: TextView
    private lateinit var tvResumoAtrasadasValor: TextView

    private lateinit var adapter: ReservaAdminAdapter
    private val todasReservas = mutableListOf<Reserva>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_reservas)

        fb = Firebase.firestore

        initViews()
        setupToolbar()
        setupRecycler()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Sempre recarrega quando volta da tela de detalhe
        carregarReservas()
    }

    private fun initViews() {
        toolbarAdminReservas = findViewById(R.id.toolbarAdminReservas)
        etBuscaReserva = findViewById(R.id.etBuscaReserva)
        btnBuscarReserva = findViewById(R.id.btnBuscarReserva)
        switchFiltrarPendentes = findViewById(R.id.switchFiltrarPendentes)
        rvReservas = findViewById(R.id.rvReservas)
        tvListaReservasVazia = findViewById(R.id.tvListaReservasVazia)
        fabNovaReserva = findViewById(R.id.fabNovaReserva)

        tvResumoPendentesValor = findViewById(R.id.tvResumoPendentesValor)
        tvResumoAprovadasValor = findViewById(R.id.tvResumoAprovadasValor)
        tvResumoAtrasadasValor = findViewById(R.id.tvResumoAtrasadasValor)
    }

    private fun setupToolbar() {
        toolbarAdminReservas.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecycler() {
        adapter = ReservaAdminAdapter(
            reservas = emptyList(),
            onClick = { reserva ->
                abrirDetalheReserva(reserva)
            },
            getUsuarioNome = { usuarioId ->
                usuariosCache[usuarioId] ?: usuarioId
            },
            getLivroTitulo = { livroId ->
                livrosCache[livroId] ?: livroId
            }
        )
        rvReservas.layoutManager = LinearLayoutManager(this)
        rvReservas.adapter = adapter
    }

    private fun setupListeners() {
        btnBuscarReserva.setOnClickListener {
            aplicarFiltrosEBusca()
        }

        etBuscaReserva.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                aplicarFiltrosEBusca()
                true
            } else {
                false
            }
        }

        switchFiltrarPendentes.setOnCheckedChangeListener { _, _ ->
            aplicarFiltrosEBusca()
        }

        fabNovaReserva.setOnClickListener {
            val intent = Intent(this, ActivityAdminNovaReserva::class.java)
            startActivity(intent)
        }
    }

    private fun carregarReservas() {
        fb.collection(COLLECTION_RESERVAS)
            .get()
            .addOnSuccessListener { snapshot ->
                todasReservas.clear()

                for (doc in snapshot.documents) {
                    val reserva = doc.toObject(Reserva::class.java) ?: continue
                    todasReservas.add(reserva)
                }

                // Depois de carregar reservas, carrega os dados de usuários e livros
                carregarUsuariosELivros {
                    aplicarFiltrosEBusca()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar reservas.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Carrega todos os usuários e livros para preencher os caches
     * e permitir que o card mostre nomeCompleto e título corretamente.
     *
     * Para o tamanho do app da faculdade, é aceitável carregar todos;
     * se crescer muito, daria pra filtrar só pelos IDs usados.
     */
    private fun carregarUsuariosELivros(onComplete: () -> Unit) {
        usuariosCache.clear()
        livrosCache.clear()

        fb.collection(COLLECTION_USERS)
            .get()
            .addOnSuccessListener { usuariosSnapshot ->
                for (doc in usuariosSnapshot.documents) {
                    val nomeCompleto = doc.getString("nomeCompleto") ?: doc.id
                    usuariosCache[doc.id] = nomeCompleto
                }

                // Depois de carregar usuários, carrega livros
                fb.collection(COLLECTION_LIVROS)
                    .get()
                    .addOnSuccessListener { livrosSnapshot ->
                        for (doc in livrosSnapshot.documents) {
                            val titulo = doc.getString("titulo") ?: doc.id
                            livrosCache[doc.id] = titulo
                        }
                        onComplete()
                    }
                    .addOnFailureListener {
                        // Mesmo se falhar, segue com o que tiver
                        onComplete()
                    }
            }
            .addOnFailureListener {
                // Se falhar ao carregar usuários, tenta pelo menos aplicar filtros
                onComplete()
            }
    }

    private fun aplicarFiltrosEBusca() {
        if (!::adapter.isInitialized) return

        val termo = etBuscaReserva.text.toString().trim()
        val termoLower = termo.lowercase(Locale.getDefault())
        val apenasPendentes = switchFiltrarPendentes.isChecked
        val agora = Date()

        // 1) Filtrar por status pendente (se o switch estiver ativo)
        val baseFiltradaPorStatus = todasReservas.filter { reserva ->
            if (apenasPendentes) {
                reserva.status == StatusReserva.PENDENTE
            } else {
                true
            }
        }

        // 2) Filtro de busca por usuário / livro (id ou campos equivalentes)
        val listaFinal = if (termoLower.isBlank()) {
            baseFiltradaPorStatus
        } else {
            baseFiltradaPorStatus.filter { reserva ->
                val usuarioCampo = usuariosCache[reserva.usuarioId]?.lowercase(Locale.getDefault())
                    ?: reserva.usuarioId.lowercase(Locale.getDefault())
                val livroCampo = livrosCache[reserva.livroId]?.lowercase(Locale.getDefault())
                    ?: reserva.livroId.lowercase(Locale.getDefault())
                usuarioCampo.contains(termoLower) || livroCampo.contains(termoLower)
            }
        }

        adapter.updateData(listaFinal)
        tvListaReservasVazia.visibility =
            if (listaFinal.isEmpty()) View.VISIBLE else View.GONE

        atualizarResumo(listaFinal, agora)
    }

    /**
     * Resumo:
     *  - Pendentes: status == PENDENTE
     *  - Atrasadas: expiraEm < agora
     *  - Aprovadas: o restante
     */
    private fun atualizarResumo(lista: List<Reserva>, agora: Date = Date()) {
        var pendentes = 0
        var aprovadas = 0
        var atrasadas = 0

        for (r in lista) {
            val expiraEmDate = r.expiraEm?.toDate()

            when {
                r.status == StatusReserva.PENDENTE -> {
                    pendentes++
                }
                expiraEmDate != null && expiraEmDate.before(agora) -> {
                    atrasadas++
                }
                else -> {
                    aprovadas++
                }
            }
        }

        tvResumoPendentesValor.text = pendentes.toString()
        tvResumoAprovadasValor.text = aprovadas.toString()
        tvResumoAtrasadasValor.text = atrasadas.toString()
    }

    private fun abrirDetalheReserva(reserva: Reserva) {
        val intent = Intent(this, ActivityAdminReservaDetalhe::class.java)
        intent.putExtra(ActivityAdminReservaDetalhe.EXTRA_RESERVA_ID, reserva.id)
        startActivity(intent)
    }

    // -----------------------------
    // Adapter da lista de reservas
    // -----------------------------
    class ReservaAdminAdapter(
        private var reservas: List<Reserva>,
        private val onClick: (Reserva) -> Unit,
        private val getUsuarioNome: (String) -> String,
        private val getLivroTitulo: (String) -> String
    ) : RecyclerView.Adapter<ReservaAdminAdapter.ReservaViewHolder>() {

        private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        inner class ReservaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvUsuario: TextView = itemView.findViewById(R.id.tvReservaUsuario)
            val tvLivro: TextView = itemView.findViewById(R.id.tvReservaLivro)
            val tvDatas: TextView = itemView.findViewById(R.id.tvReservaDatas)
            val tvStatus: TextView = itemView.findViewById(R.id.tvReservaStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_reserva_admin, parent, false)
            return ReservaViewHolder(view)
        }

        override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
            val reserva = reservas[position]

            // Usa os nomes/títulos vindos das collections users e livros via cache
            holder.tvUsuario.text = getUsuarioNome(reserva.usuarioId)
            holder.tvLivro.text = getLivroTitulo(reserva.livroId)

            val dataEmprestimo = reserva.dataReserva?.toDate()?.let { sdf.format(it) } ?: "-"
            val dataDevolucao = reserva.expiraEm?.toDate()?.let { sdf.format(it) } ?: "-"

            holder.tvDatas.text = "Emp.: $dataEmprestimo • Dev.: $dataDevolucao"
            holder.tvStatus.text = reserva.status.name

            holder.itemView.setOnClickListener { onClick(reserva) }
        }

        override fun getItemCount(): Int = reservas.size

        fun updateData(novaLista: List<Reserva>) {
            reservas = novaLista
            notifyDataSetChanged()
        }
    }
}
