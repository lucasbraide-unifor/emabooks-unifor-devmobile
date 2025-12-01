package com.example.emabooks

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ActivityMinhaConta : AppCompatActivity() {

    // HEADER
    private lateinit var btnLogout: ImageButton
    private lateinit var tvHeaderTitleMinhaConta: TextView

    // INFO USUÁRIO
    private lateinit var tvNomeUsuario: TextView
    private lateinit var tvEmailUsuario: TextView
    private lateinit var tvMatriculaUsuario: TextView
    private lateinit var tvMembroDesde: TextView
    private lateinit var tvQtdEmprestimos: TextView
    private lateinit var tvQtdFavoritos: TextView

    // CONFIGURAÇÕES
    private lateinit var spinnerTamanhoFonte: Spinner
    private lateinit var spinnerContraste: Spinner
    private lateinit var switchNotificacoes: Switch

    // FAVORITOS
    private lateinit var rvFavoritos: RecyclerView
    private lateinit var tvSemFavoritos: TextView

    // BOTTOM NAV
    private lateinit var bottomNavigationMinhaConta: BottomNavigationView

    // FIRESTORE
    private lateinit var fb: FirebaseFirestore

    // DADOS
    private var userId: String? = null
    private val listaFavoritos = mutableListOf<Livro>()
    private lateinit var favoritosAdapter: FavoriteBooksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minha_conta)

        fb = FirebaseFirestore.getInstance()

        // Verifica login
        userId = SessionManager.obterUsuarioId(this)
        if (userId == null) {
            redirecionarParaLogin()
            return
        }

        bindViews()
        setupBottomNavigation()
        setupConfigControls()
        setupFavoritosRecycler()
        setupLogout()

        AppConfigManager.applyUserConfig(this)

        carregarDadosUsuario()
        carregarResumoEmprestimos()
        carregarFavoritos()
    }

    // ---------------- BIND VIEWS ----------------

    private fun bindViews() {
        // Header
        btnLogout = findViewById(R.id.btnLogout)
        tvHeaderTitleMinhaConta = findViewById(R.id.tvHeaderTitleMinhaConta)

        // Info usuário
        tvNomeUsuario = findViewById(R.id.tvNomeUsuario)
        tvEmailUsuario = findViewById(R.id.tvEmailUsuario)
        tvMatriculaUsuario = findViewById(R.id.tvMatriculaUsuario)
        tvMembroDesde = findViewById(R.id.tvMembroDesde)
        tvQtdEmprestimos = findViewById(R.id.tvQtdEmprestimos)
        tvQtdFavoritos = findViewById(R.id.tvQtdFavoritos)

        // Configurações
        spinnerTamanhoFonte = findViewById(R.id.spinnerTamanhoFonte)
        spinnerContraste = findViewById(R.id.spinnerContraste)
        switchNotificacoes = findViewById(R.id.switchNotificacoes)

        // Favoritos
        rvFavoritos = findViewById(R.id.rvFavoritos)
        tvSemFavoritos = findViewById(R.id.tvSemFavoritos)

        // Bottom nav
        bottomNavigationMinhaConta = findViewById(R.id.bottomNavigationMinhaConta)
    }

    // ---------------- BOTTOM NAV ----------------

    private fun setupBottomNavigation() {
        bottomNavigationMinhaConta.selectedItemId = R.id.nav_minha_conta

        bottomNavigationMinhaConta.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_acervo -> {
                    val intent = Intent(this, ActivityHome::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }
                R.id.nav_emprestimos -> {
                    val intent = Intent(this, ActivityReservas::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }
                R.id.nav_minha_conta -> {
                    // Já está aqui
                    true
                }
                else -> false
            }
        }
    }

    // ---------------- LOGOUT ----------------

    private fun setupLogout() {
        btnLogout.setOnClickListener {
            // Se tiver método de limpar sessão, chama aqui
            // SessionManager.limparSessao(this)

            val intent = Intent(this, ActivityLogin::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun redirecionarParaLogin() {
        Toast.makeText(this, "Faça login para acessar Minha Conta.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, ActivityLogin::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ---------------- CONFIGURAÇÕES ----------------

    private fun setupConfigControls() {
        val tamanhosFonte = arrayOf("Padrão", "Médio", "Grande")
        val contrastes = arrayOf("Padrão", "Alto contraste")

        val fontAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tamanhosFonte
        )
        spinnerTamanhoFonte.adapter = fontAdapter

        val contrastAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            contrastes
        )
        spinnerContraste.adapter = contrastAdapter

        val tamanhoIdx = AppConfigManager.getFontSizeIndex(this)
        val contrasteIdx = AppConfigManager.getContrastIndex(this)
        val notificacoesAtivas = AppConfigManager.isNotificationsEnabled(this)

        spinnerTamanhoFonte.setSelection(tamanhoIdx.coerceIn(0, tamanhosFonte.lastIndex))
        spinnerContraste.setSelection(contrasteIdx.coerceIn(0, contrastes.lastIndex))
        switchNotificacoes.isChecked = notificacoesAtivas

        spinnerTamanhoFonte.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                AppConfigManager.setFontSizeIndex(this@ActivityMinhaConta, position)
                AppConfigManager.applyUserConfig(this@ActivityMinhaConta)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerContraste.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                AppConfigManager.setContrastIndex(this@ActivityMinhaConta, position)
                AppConfigManager.applyUserConfig(this@ActivityMinhaConta)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        switchNotificacoes.setOnCheckedChangeListener { _, isChecked ->
            AppConfigManager.setNotificationsEnabled(this, isChecked)
        }
    }

    // ---------------- FAVORITOS ----------------

    private fun setupFavoritosRecycler() {
        favoritosAdapter = FavoriteBooksAdapter(listaFavoritos) { livro ->
            val intent = Intent(this, ActivityBookDetails::class.java)
            intent.putExtra(ActivityBookDetails.EXTRA_LIVRO, livro)
            startActivity(intent)
        }
        rvFavoritos.layoutManager = LinearLayoutManager(this)
        rvFavoritos.adapter = favoritosAdapter
    }

    // ---------------- FIRESTORE: USUÁRIO ----------------

    private fun carregarDadosUsuario() {
        val uid = userId ?: return

        fb.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val nome = doc.getString("nomeCompleto") ?: "Não informado"
                val email = doc.getString("email") ?: "Não informado"
                val matricula = doc.getString("matricula") ?: "Não informado"
                val createdAt = doc.getTimestamp("createdAt")

                val membroDesde = createdAt?.toDate()?.let {
                    android.text.format.DateFormat.format("dd/MM/yyyy", it).toString()
                } ?: "Não informado"

                tvNomeUsuario.text = nome
                tvEmailUsuario.text = email
                tvMatriculaUsuario.text = matricula
                tvMembroDesde.text = "Membro desde: $membroDesde"
            }
    }

    private fun carregarResumoEmprestimos() {
        val uid = userId ?: return

        fb.collection("emprestimos")
            .whereEqualTo("usuarioId", uid)
            .get()
            .addOnSuccessListener { snap ->
                val qtd = snap.size()
                tvQtdEmprestimos.text = "Empréstimos realizados: $qtd"
            }
    }

    private fun carregarFavoritos() {
        val uid = userId ?: return

        fb.collection("favoritos")
            .whereEqualTo("usuarioId", uid)
            .get()
            .addOnSuccessListener { snap ->
                val livroIds = snap.documents
                    .mapNotNull { it.getString("livroId") }
                    .distinct()

                if (livroIds.isEmpty()) {
                    listaFavoritos.clear()
                    favoritosAdapter.notifyDataSetChanged()
                    tvSemFavoritos.visibility = View.VISIBLE
                    tvQtdFavoritos.text = "Favoritos: 0"
                    return@addOnSuccessListener
                }

                val tasks = livroIds.map { id ->
                    fb.collection("livros").document(id).get()
                }

                Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
                    .addOnSuccessListener { docs ->
                        listaFavoritos.clear()
                        for (d in docs) {
                            val livro = d.toObject(Livro::class.java)
                            if (livro != null) {
                                listaFavoritos.add(livro.copy(id = d.id))
                            }
                        }
                        favoritosAdapter.notifyDataSetChanged()
                        tvSemFavoritos.visibility =
                            if (listaFavoritos.isEmpty()) View.VISIBLE else View.GONE

                        tvQtdFavoritos.text = "Favoritos: ${listaFavoritos.size}"
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Erro ao carregar favoritos.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao consultar favoritos.", Toast.LENGTH_SHORT).show()
            }
    }
}

// ---------------- ADAPTER FAVORITOS ----------------

class FavoriteBooksAdapter(
    private val itens: List<Livro>,
    private val onBookClick: (Livro) -> Unit
) : RecyclerView.Adapter<FavoriteBooksAdapter.FavoriteBookViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteBookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_livro, parent, false)
        return FavoriteBookViewHolder(view, onBookClick)
    }

    override fun onBindViewHolder(holder: FavoriteBookViewHolder, position: Int) {
        holder.bind(itens[position])
    }

    override fun getItemCount(): Int = itens.size

    class FavoriteBookViewHolder(
        itemView: View,
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

            if (!livro.autor.isNullOrBlank()) {
                tvAutor.text = livro.autor
                tvAutor.visibility = View.VISIBLE
            } else {
                tvAutor.text = ""
                tvAutor.visibility = View.GONE
            }

            if (livro.anoPublicacao != null) {
                tvAno.text = livro.anoPublicacao.toString()
                tvAno.visibility = View.VISIBLE
            } else {
                tvAno.text = ""
                tvAno.visibility = View.GONE
            }

            // Mesma lógica de disponibilidade da ActivityHome, mas aqui local
            val disponivel = if (livro.statusGeral != StatusLivro.DISPONIVEL) {
                false
            } else {
                when (livro.tipoAcervo) {
                    TipoAcervo.FISICO -> true
                    TipoAcervo.DIGITAL, TipoAcervo.HIBRIDO -> true
                }
            }

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
                // Futuro: Glide/Picasso
                ivCapa.setImageResource(R.drawable.ic_book_placeholder)
            }
        }
    }
}