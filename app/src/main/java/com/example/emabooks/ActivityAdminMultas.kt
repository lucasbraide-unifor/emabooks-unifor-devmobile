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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityAdminMultas : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_MULTAS = "multas"
    private val COLLECTION_USERS = "users"
    private val COLLECTION_LIVROS = "livros"

    // Cache de nomes e títulos
    private val usuariosCache = mutableMapOf<String, String>()
    private val livrosCache = mutableMapOf<String, String>()

    // Views
    private lateinit var toolbarAdminMultas: MaterialToolbar
    private lateinit var etBuscaMultaUsuario: EditText
    private lateinit var btnBuscarMulta: MaterialButton
    private lateinit var switchApenasPendentes: SwitchMaterial
    private lateinit var rvMultas: RecyclerView
    private lateinit var tvListaMultasVazia: TextView
    private lateinit var fabNovaMulta: FloatingActionButton

    private lateinit var adapter: MultaAdminAdapter
    private val todasMultas = mutableListOf<Multa>()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_multas)

        fb = Firebase.firestore

        bindViews()
        setupToolbar()
        setupRecycler()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        carregarMultas()
    }

    private fun bindViews() {
        toolbarAdminMultas = findViewById(R.id.toolbarAdminMultas)
        etBuscaMultaUsuario = findViewById(R.id.etBuscaMultaUsuario)
        btnBuscarMulta = findViewById(R.id.btnBuscarMulta)
        switchApenasPendentes = findViewById(R.id.switchApenasPendentes)
        rvMultas = findViewById(R.id.rvMultas)
        tvListaMultasVazia = findViewById(R.id.tvListaMultasVazia)
        fabNovaMulta = findViewById(R.id.fabNovaMulta)
    }

    private fun setupToolbar() {
        toolbarAdminMultas.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecycler() {
        adapter = MultaAdminAdapter(
            multas = emptyList(),
            getUsuarioNome = { usuarioId ->
                usuariosCache[usuarioId] ?: usuarioId
            },
            getLivroTitulo = { livroId ->
                livroId?.let { livrosCache[it] ?: it } ?: "-"
            },
            onQuitarMulta = { multa -> confirmarQuitarMulta(multa) },
            onRemoverMulta = { multa -> confirmarRemoverMulta(multa) },
            onClickMulta = { multa -> abrirDetalheMulta(multa) }
        )
        rvMultas.layoutManager = LinearLayoutManager(this)
        rvMultas.adapter = adapter
    }

    private fun setupListeners() {
        btnBuscarMulta.setOnClickListener {
            aplicarFiltrosEBusca()
        }

        etBuscaMultaUsuario.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                aplicarFiltrosEBusca()
                true
            } else {
                false
            }
        }

        switchApenasPendentes.setOnCheckedChangeListener { _, _ ->
            aplicarFiltrosEBusca()
        }

        fabNovaMulta.setOnClickListener {
            // Tela de aplicar multa (Activity de criação/edição de multa)
            val intent = Intent(this, ActivityAdminNovaMulta::class.java)
            startActivity(intent)
        }
    }

    private fun carregarMultas() {
        fb.collection(COLLECTION_MULTAS)
            .get()
            .addOnSuccessListener { snapshot ->
                todasMultas.clear()

                for (doc in snapshot.documents) {
                    val multa = doc.toObject(Multa::class.java) ?: continue
                    multa.id = doc.id
                    todasMultas.add(multa)
                }

                // Carrega caches de users e livros antes de aplicar filtros
                carregarUsuariosELivros {
                    aplicarFiltrosEBusca()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar multas.", Toast.LENGTH_SHORT).show()
            }
    }

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
                        onComplete()
                    }
            }
            .addOnFailureListener {
                onComplete()
            }
    }

    private fun aplicarFiltrosEBusca() {
        if (!::adapter.isInitialized) return

        val termo = etBuscaMultaUsuario.text.toString().trim()
        val termoLower = termo.lowercase(Locale.getDefault())
        val apenasPendentes = switchApenasPendentes.isChecked

        // Filtro por status
        var lista = todasMultas.filter { multa ->
            if (apenasPendentes) {
                multa.status.equals("PENDENTE", ignoreCase = true)
            } else {
                true
            }
        }

        // Busca por usuário (nome ou id)
        if (termoLower.isNotEmpty()) {
            lista = lista.filter { multa ->
                val usuarioNome = usuariosCache[multa.usuarioId]?.lowercase(Locale.getDefault())
                    ?: multa.usuarioId.lowercase(Locale.getDefault())
                usuarioNome.contains(termoLower)
            }
        }

        // Ordenação por data (mais recentes primeiro)
        lista = lista.sortedByDescending { multa ->
            multa.dataMulta?.toDate() ?: Date(0)
        }

        adapter.updateData(lista)
        tvListaMultasVazia.visibility =
            if (lista.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmarQuitarMulta(multa: Multa) {
        if (!multa.status.equals("PENDENTE", ignoreCase = true)) {
            Toast.makeText(this, "Apenas multas pendentes podem ser quitadas.", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Quitar multa")
            .setMessage("Confirmar quitação desta multa?")
            .setPositiveButton("Confirmar") { _, _ ->
                quitarMulta(multa)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun quitarMulta(multa: Multa) {
        fb.collection(COLLECTION_MULTAS)
            .document(multa.id)
            .update("status", "PAGA")
            .addOnSuccessListener {
                Toast.makeText(this, "Multa quitada com sucesso.", Toast.LENGTH_SHORT).show()
                carregarMultas()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao quitar multa.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmarRemoverMulta(multa: Multa) {
        if (multa.emRegularizacao == true) {
            Toast.makeText(
                this,
                "Não é possível remover multa em processo de regularização.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Remover multa")
            .setMessage("Tem certeza que deseja remover esta multa?")
            .setPositiveButton("Remover") { _, _ ->
                removerMulta(multa)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun removerMulta(multa: Multa) {
        fb.collection(COLLECTION_MULTAS)
            .document(multa.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Multa removida.", Toast.LENGTH_SHORT).show()
                carregarMultas()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao remover multa.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun abrirDetalheMulta(multa: Multa) {
        val intent = Intent(this, ActivityAdminMultaDetalhe::class.java)
        intent.putExtra("EXTRA_MULTA_ID", multa.id)
        startActivity(intent)
    }

    // -----------------------------
    // Adapter da lista de multas
    // -----------------------------
    class MultaAdminAdapter(
        private var multas: List<Multa>,
        private val getUsuarioNome: (String) -> String,
        private val getLivroTitulo: (String?) -> String,
        private val onQuitarMulta: (Multa) -> Unit,
        private val onRemoverMulta: (Multa) -> Unit,
        private val onClickMulta: (Multa) -> Unit
    ) : RecyclerView.Adapter<MultaAdminAdapter.MultaViewHolder>() {

        private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        inner class MultaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvUsuario: TextView = itemView.findViewById(R.id.tvMultaUsuario)
            val tvLivro: TextView = itemView.findViewById(R.id.tvMultaLivro)
            val tvReserva: TextView = itemView.findViewById(R.id.tvMultaReserva)
            val tvDescricao: TextView = itemView.findViewById(R.id.tvMultaDescricao)
            val tvValor: TextView = itemView.findViewById(R.id.tvMultaValor)
            val tvData: TextView = itemView.findViewById(R.id.tvMultaData)
            val tvStatus: TextView = itemView.findViewById(R.id.tvMultaStatus)
            val btnQuitar: MaterialButton = itemView.findViewById(R.id.btnQuitarMulta)
            val btnRemover: MaterialButton = itemView.findViewById(R.id.btnRemoverMulta)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultaViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_multa_admin, parent, false)
            return MultaViewHolder(view)
        }

        override fun onBindViewHolder(holder: MultaViewHolder, position: Int) {
            val multa = multas[position]

            holder.tvUsuario.text = getUsuarioNome(multa.usuarioId)
            holder.tvLivro.text = getLivroTitulo(multa.livroId)
            holder.tvReserva.text = multa.reservaId?.let { "Reserva: $it" } ?: "Reserva: -"
            holder.tvDescricao.text = multa.descricao

            holder.tvValor.text = currencyFormat.format(multa.valor)

            val data = multa.dataMulta?.toDate()
            holder.tvData.text = data?.let { sdf.format(it) } ?: "-"

            val statusLabel = if (multa.status.equals("PAGA", ignoreCase = true)) {
                "Paga"
            } else {
                "Pendente"
            }
            holder.tvStatus.text = statusLabel

            // Visibilidade dos botões com base no status
            if (multa.status.equals("PENDENTE", ignoreCase = true)) {
                holder.btnQuitar.visibility = View.VISIBLE
            } else {
                holder.btnQuitar.visibility = View.GONE
            }

            holder.btnQuitar.setOnClickListener {
                onQuitarMulta(multa)
            }

            holder.btnRemover.setOnClickListener {
                onRemoverMulta(multa)
            }

            holder.itemView.setOnClickListener {
                onClickMulta(multa)
            }
        }

        override fun getItemCount(): Int = multas.size

        fun updateData(novaLista: List<Multa>) {
            multas = novaLista
            notifyDataSetChanged()
        }
    }
}

/**
 * Modelo de Multa para Firestore.
 * Considere emprestimo como sendo reserva (reservaId).
 */
data class Multa(
    var id: String = "",
    var usuarioId: String = "",
    var livroId: String? = null,
    var reservaId: String? = null,
    var descricao: String = "",
    var valor: Double = 0.0,
    var dataMulta: Timestamp? = null,
    var status: String = "PENDENTE",
    var emRegularizacao: Boolean? = false
)