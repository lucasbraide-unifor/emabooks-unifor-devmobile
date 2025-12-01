package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
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

// Lista em memória + adapter
private val listaCompleta = mutableListOf<Usuario>()
private val listaFiltrada = mutableListOf<Usuario>()
private lateinit var usuariosAdapter: UsuariosAdapter

// Filtro atual (status + tipo)
private var filtroAtual = Filtro.TODOS

private enum class Filtro {
    TODOS,
    ATIVOS,
    INATIVOS,
    ADMINS,
    USUARIOS
}

class ActivityAdminUsuarios : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_USERS = "users" // ajuste se o nome for outro

    // Views
    private lateinit var toolbarUsuarios: MaterialToolbar
    private lateinit var layoutUsuariosContent: LinearLayout
    private lateinit var editTextSearch: EditText
    private lateinit var textViewQtdUsuarios: TextView
    private lateinit var textEstatUsuarios: TextView
    private lateinit var textEstatAdmins: TextView
    private lateinit var textEstatInativos: TextView
    private lateinit var rvUsuarios: RecyclerView
    private lateinit var buttonAdicionar: MaterialButton
    private lateinit var buttonAnterior: MaterialButton
    private lateinit var buttonProxima: MaterialButton
    private lateinit var textPaginaInfo: TextView

    private lateinit var buttonFiltros: MaterialButton


    // Lista em memória + adapter
    private val listaCompleta = mutableListOf<Usuario>()
    private val listaFiltrada = mutableListOf<Usuario>()
    private lateinit var usuariosAdapter: UsuariosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_usuarios)

        fb = Firebase.firestore

        bindViews()
        setupToolbar()
        setupRecycler()
        setupSearch()
        setupButtons()

        carregarUsuarios()
    }

    override fun onResume() {
        super.onResume()
        carregarUsuarios()
    }

    private fun bindViews() {
        toolbarUsuarios = findViewById(R.id.toolbarUsuarios)
        layoutUsuariosContent = findViewById(R.id.layoutUsuariosContent)
        editTextSearch = findViewById(R.id.editTextSearch)
        textViewQtdUsuarios = findViewById(R.id.textViewQtdUsuarios)
        textEstatUsuarios = findViewById(R.id.textEstatUsuarios)
        textEstatAdmins = findViewById(R.id.textEstatAdmins)
        textEstatInativos = findViewById(R.id.textEstatInativos)
        rvUsuarios = findViewById(R.id.rvUsuarios)
        buttonAdicionar = findViewById(R.id.buttonAdicionar)
        buttonAnterior = findViewById(R.id.buttonAnterior)
        buttonProxima = findViewById(R.id.buttonProxima)
        textPaginaInfo = findViewById(R.id.textPaginaInfo)
        buttonFiltros = findViewById(R.id.buttonFiltros)

        buttonAnterior.isEnabled = false
        buttonProxima.isEnabled = false
        textPaginaInfo.text = "Página 1"
    }

    private fun setupToolbar() {
        toolbarUsuarios.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecycler() {
        usuariosAdapter = UsuariosAdapter(
            itens = listaFiltrada,
            onItemClick = { usuario ->
                val intent = Intent(this, ActivityAdminUsuarioDetalhe::class.java)
                intent.putExtra(ActivityAdminUsuarioDetalhe.EXTRA_USUARIO_ID, usuario.id)
                startActivity(intent)
            }
        )

        rvUsuarios.apply {
            layoutManager = LinearLayoutManager(this@ActivityAdminUsuarios)
            adapter = usuariosAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSearch() {
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrarUsuarios(s?.toString().orEmpty())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupButtons() {
        buttonFiltros.setOnClickListener {
            filtroAtual = when (filtroAtual) {
                Filtro.TODOS -> Filtro.ATIVOS
                Filtro.ATIVOS -> Filtro.INATIVOS
                Filtro.INATIVOS -> Filtro.ADMINS
                Filtro.ADMINS -> Filtro.USUARIOS
                Filtro.USUARIOS -> Filtro.TODOS
            }

            // Atualiza texto do botão conforme filtro
            buttonFiltros.text = when (filtroAtual) {
                Filtro.TODOS -> "Filtros: Todos"
                Filtro.ATIVOS -> "Filtros: Ativos"
                Filtro.INATIVOS -> "Filtros: Inativos"
                Filtro.ADMINS -> "Filtros: Admins"
                Filtro.USUARIOS -> "Filtros: Usuários"
            }

            // Reaplica o filtro usando o texto atual da busca
            filtrarUsuarios(editTextSearch.text.toString())
        }

        buttonAdicionar.setOnClickListener {
            // tela de novo usuário (vai criar depois)
            val intent = Intent(this, ActivityAdicionarUsuario::class.java)
            startActivity(intent)
        }

        buttonAnterior.setOnClickListener { /* futuro: paginação */ }
        buttonProxima.setOnClickListener { /* futuro: paginação */ }
    }

    private fun carregarUsuarios() {
        fb.collection(COLLECTION_USERS)
            .get()
            .addOnSuccessListener { snapshot ->
                listaCompleta.clear()

                for (doc in snapshot.documents) {
                    val usuario = doc.toObject(Usuario::class.java)
                    if (usuario != null) {
                        listaCompleta.add(usuario.copy(id = doc.id))
                    }
                }

                aplicarFiltro("")
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao carregar usuários: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun filtrarUsuarios(query: String) {
        aplicarFiltro(query.trim())
    }

    private fun aplicarFiltro(query: String) {
        val q = query.lowercase()

        listaFiltrada.clear()

        val filtrados = listaCompleta.filter { u ->
            val nome = u.nomeCompleto ?: ""
            val email = u.email ?: ""

            val matchesSearch =
                q.isEmpty() ||
                        nome.lowercase().contains(q) ||
                        email.lowercase().contains(q)

            val isAtivo = u.ativo != false       // null conta como ativo
            val isAdmin = u.isAdmin == true

            val matchesFiltro = when (filtroAtual) {
                Filtro.TODOS -> true
                Filtro.ATIVOS -> isAtivo
                Filtro.INATIVOS -> !isAtivo
                Filtro.ADMINS -> isAdmin
                Filtro.USUARIOS -> !isAdmin
            }

            matchesSearch && matchesFiltro
        }

        listaFiltrada.addAll(filtrados)

        usuariosAdapter.notifyDataSetChanged()
        atualizarResumo()
    }

    private fun atualizarResumo() {
        val total = listaFiltrada.size
        val admins = listaFiltrada.count { it.isAdmin }
        val inativos = listaFiltrada.count { !it.ativo }
        val usuariosComuns = total - admins

        textViewQtdUsuarios.text = "$total usuário(s) encontrado(s)"
        textEstatUsuarios.text = usuariosComuns.toString()
        textEstatAdmins.text = admins.toString()
        textEstatInativos.text = inativos.toString()
    }
}

// -------- ADAPTER NO MESMO ARQUIVO --------

class UsuariosAdapter(
    private val itens: List<Usuario>,
    private val onItemClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        holder.bind(itens[position])
    }

    override fun getItemCount(): Int = itens.size

    class UsuarioViewHolder(
        itemView: View,
        private val onItemClick: (Usuario) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val textNome: TextView = itemView.findViewById(R.id.textUsuarioNome)
        private val textEmail: TextView = itemView.findViewById(R.id.textUsuarioEmail)
        private val textMatricula: TextView = itemView.findViewById(R.id.textUsuarioMatricula)
        private val chipPapel: TextView = itemView.findViewById(R.id.chipPapel)
        private val chipStatus: TextView = itemView.findViewById(R.id.chipStatus)
        private val buttonMenu: ImageButton = itemView.findViewById(R.id.buttonMenuUsuario)


        private var currentUsuario: Usuario? = null

        init {
            itemView.setOnClickListener {
                currentUsuario?.let { u -> onItemClick(u) }
            }

            buttonMenu.setOnClickListener {
                // futuro: PopupMenu com ações
            }
        }

        fun bind(usuario: Usuario) {
            currentUsuario = usuario

            textNome.text = usuario.nomeCompleto ?: "Nome não informado"
            textEmail.text = usuario.email ?: "-"
            textMatricula.text = "Matrícula: ${usuario.matricula ?: "-"}"

            val isAdmin = usuario.isAdmin == true
            val isAtivo = usuario.ativo != false

            // Tipo de usuário
            if (isAdmin) {
                chipPapel.text = "Admin"
                chipPapel.setBackgroundColor(0xFFFCE7F3.toInt())
                chipPapel.setTextColor(0xFFBE185D.toInt())
            } else {
                chipPapel.text = "Usuário"
                chipPapel.setBackgroundColor(0xFFEEF2FF.toInt())
                chipPapel.setTextColor(0xFF4F46E5.toInt())
            }

            // Status
            if (isAtivo) {
                chipStatus.text = "Ativo"
                chipStatus.setBackgroundColor(0xFFDCFCE7.toInt())
                chipStatus.setTextColor(0xFF166534.toInt())
            } else {
                chipStatus.text = "Inativo"
                chipStatus.setBackgroundColor(0xFFFEE2E2.toInt())
                chipStatus.setTextColor(0xFFB91C1C.toInt())
            }
        }
    }
}