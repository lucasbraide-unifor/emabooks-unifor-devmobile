package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ActivityReservas : BaseActivity() {

    // Views
    private lateinit var tvCarregandoReservas: TextView
    private lateinit var tvSemReservas: TextView
    private lateinit var tvFiltroStatusReserva: TextView
    private lateinit var btnMinhasMultas: MaterialButton
    private lateinit var rvReservas: RecyclerView
    private lateinit var bottomNavigationReservas: BottomNavigationView

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val collectionName = "reservas"

    // Dados
    private val listaOriginal = mutableListOf<Reserva>()
    private val listaFiltrada = mutableListOf<Reserva>()
    private lateinit var adapter: ReservaAdapter

    // Mapa livroId -> Livro para enriquecer o card
    private val livrosPorId = mutableMapOf<String, Livro>()

    private var filtroSelecionado: StatusReserva? = null

    private val labelsStatus = listOf(
        "Todos",
        "Ativa",
        "Cancelada",
        "Convertida",
        "Pendente"
    )

    private val valoresStatus = listOf(
        null,
        StatusReserva.ATIVA,
        StatusReserva.CANCELADA,
        StatusReserva.CONVERTIDA,
        StatusReserva.PENDENTE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservas)

        fb = FirebaseFirestore.getInstance()

        bindViews()
        setupRecycler()
        setupFiltroStatus()
        setupMinhasMultas()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Atualiza a lista sempre que voltar para essa tela
        carregarReservasDoUsuario()
    }

    private fun bindViews() {
        tvCarregandoReservas = findViewById(R.id.tvCarregandoReservas)
        tvSemReservas = findViewById(R.id.tvSemReservas)
        tvFiltroStatusReserva = findViewById(R.id.tvFiltroStatusReserva)
        btnMinhasMultas = findViewById(R.id.btnMinhasMultas)
        rvReservas = findViewById(R.id.rvReservas)
        bottomNavigationReservas = findViewById(R.id.bottomNavigationReservas)
    }

    private fun setupRecycler() {
        adapter = ReservaAdapter(
            itens = listaFiltrada,
            livrosPorId = livrosPorId
        ) { reserva ->
            // 👉 Ao clicar no card de reserva, abre ActivityReservaDetails
            val intent = Intent(this, ActivityReservaDetails::class.java)
            intent.putExtra(ActivityReservaDetails.EXTRA_RESERVA_ID, reserva.id)
            startActivity(intent)
        }
        rvReservas.layoutManager = LinearLayoutManager(this)
        rvReservas.adapter = adapter
    }

    private fun setupFiltroStatus() {
        tvFiltroStatusReserva.setOnClickListener {
            abrirDialogFiltro()
        }
    }

    private fun setupMinhasMultas() {
        btnMinhasMultas.setOnClickListener {
            val intent = Intent(this, ActivityMinhasMultas::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        // Marca o item "Empréstimos" como selecionado (neste contexto = Reservas)
        bottomNavigationReservas.selectedItemId = R.id.nav_emprestimos

        bottomNavigationReservas.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_acervo -> {
                    // Volta para a Activity principal do acervo
                    val intent = Intent(this, ActivityHome::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }
                R.id.nav_emprestimos -> {
                    // Já estamos na tela de reservas/emprestimos
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

    // -------------------- FILTRO --------------------

    private fun abrirDialogFiltro() {
        val indiceInicial = valoresStatus.indexOf(filtroSelecionado)

        AlertDialog.Builder(this)
            .setTitle("Filtrar por status")
            .setSingleChoiceItems(labelsStatus.toTypedArray(), indiceInicial) { dialog, which ->
                filtroSelecionado = valoresStatus[which]
                atualizarTextoFiltro()
                aplicarFiltro()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarTextoFiltro() {
        tvFiltroStatusReserva.text =
            if (filtroSelecionado == null) "Status: Todos"
            else "Status: ${filtroSelecionado.toString().lowercase().replaceFirstChar { it.uppercase() }}"
    }

    // -------------------- FIRESTORE --------------------

    private fun carregarReservasDoUsuario() {
        tvCarregandoReservas.visibility = View.VISIBLE
        tvSemReservas.visibility = View.GONE

        val userId = SessionManager.obterUsuarioId(this)
        if (userId == null) {
            tvCarregandoReservas.visibility = View.GONE
            listaOriginal.clear()
            listaFiltrada.clear()
            adapter.notifyDataSetChanged()
            tvSemReservas.visibility = View.VISIBLE
            Toast.makeText(this, "Faça login para ver suas reservas.", Toast.LENGTH_LONG).show()
            return
        }

        fb.collection(collectionName)
            .whereEqualTo("usuarioId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                listaOriginal.clear()

                for (doc in snapshot.documents) {
                    val reserva = doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                    if (reserva != null) listaOriginal.add(reserva)
                }

                tvCarregandoReservas.visibility = View.GONE

                // Aplica filtro de status nas reservas
                aplicarFiltro()

                // Carrega os dados dos livros relacionados
                carregarLivrosDasReservas(listaOriginal)
            }
            .addOnFailureListener { e ->
                tvCarregandoReservas.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar reservas: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun carregarLivrosDasReservas(reservas: List<Reserva>) {
        val livroIds = reservas.map { it.livroId }
            .filter { it.isNotBlank() }
            .distinct()

        if (livroIds.isEmpty()) {
            livrosPorId.clear()
            adapter.notifyDataSetChanged()
            return
        }

        val tasks = livroIds.map { id ->
            fb.collection("livros").document(id).get()
        }

        Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
            .addOnSuccessListener { docs ->
                livrosPorId.clear()
                for (snap in docs) {
                    val livro = snap.toObject(Livro::class.java)
                    if (livro != null) {
                        val id = snap.id
                        livrosPorId[id] = livro.copy(id = id)
                    }
                }
                // Atualiza os cards com título/autor
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar dados dos livros: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun aplicarFiltro() {
        listaFiltrada.clear()

        val filtrados = if (filtroSelecionado == null) listaOriginal
        else listaOriginal.filter { it.status == filtroSelecionado }

        listaFiltrada.addAll(filtrados)
        adapter.notifyDataSetChanged()

        tvSemReservas.visibility =
            if (listaFiltrada.isEmpty()) View.VISIBLE else View.GONE
    }
}

// -------------------------------------------------------------
// ADAPTER
// -------------------------------------------------------------
class ReservaAdapter(
    private val itens: List<Reserva>,
    private val livrosPorId: Map<String, Livro>,
    private val onItemClick: (Reserva) -> Unit
) : RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reserva, parent, false)
        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        val item = itens[position]
        val livro = livrosPorId[item.livroId]
        holder.bind(item, livro)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = itens.size

    class ReservaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTituloReserva: TextView = itemView.findViewById(R.id.tvTituloReserva)
        private val tvStatusReserva: TextView = itemView.findViewById(R.id.tvStatusReserva)
        private val tvPosicaoFila: TextView = itemView.findViewById(R.id.tvPosicaoFila)
        private val tvExpiraEm: TextView = itemView.findViewById(R.id.tvExpiraEm)
        private val tvBadgeExpirando: TextView = itemView.findViewById(R.id.tvBadgeExpirando)

        fun bind(reserva: Reserva, livro: Livro?) {
            // Título do card: título do livro (fallback para ID se não achar)
            tvTituloReserva.text = livro?.titulo ?: "Livro não encontrado"

            // Status da reserva
            tvStatusReserva.text = "Status da reserva: ${reserva.status}"

            // Autor + posição na fila (se autor existir)
            val autor = livro?.autor
            tvPosicaoFila.text = buildString {
                if (!autor.isNullOrBlank()) {
                    append("Autor: $autor")
                }
                if (reserva.posicaoFila > 0) {
                    if (isNotEmpty()) append(" • ")
                    append("Posição na fila: ${reserva.posicaoFila}")
                }
            }.ifBlank {
                "Posição na fila: ${reserva.posicaoFila}"
            }

            val expira = reserva.expiraEm?.toDate()
            tvExpiraEm.text = if (expira != null)
                "Expira em: ${android.text.format.DateFormat.format("dd/MM/yyyy", expira)}"
            else
                "Sem data de expiração"

            if (expira != null) {
                val hoje = java.util.Date().time
                val diffDias = ((expira.time - hoje) / (1000 * 60 * 60 * 24)).toInt()

                when {
                    diffDias < 0 -> {
                        tvBadgeExpirando.visibility = View.VISIBLE
                        tvBadgeExpirando.text = "EXPIRADA"
                        tvBadgeExpirando.setBackgroundColor(0xFFD32F2F.toInt())
                    }
                    diffDias in 0..1 -> {
                        tvBadgeExpirando.visibility = View.VISIBLE
                        tvBadgeExpirando.text = "EXPIRANDO"
                        tvBadgeExpirando.setBackgroundColor(0xFFFFA000.toInt())
                    }
                    else -> tvBadgeExpirando.visibility = View.GONE
                }
            } else {
                tvBadgeExpirando.visibility = View.GONE
            }
        }
    }
}