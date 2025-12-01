package com.example.emabooks

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.core.net.toUri

class ActivityBookDetails : BaseActivity() {

    private fun textoInfoComNegrito(l: Livro): CharSequence {
        val builder = android.text.SpannableStringBuilder()

        fun addLinha(rotulo: String, valor: String) {
            val start = builder.length
            builder.append("$rotulo: ")
            builder.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start,
                start + rotulo.length + 1, // inclui ":"
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(valor)
            builder.append("\n")
        }

        addLinha("Ano", l.anoPublicacao?.toString() ?: "Não informado")
        addLinha("Editora", l.editora ?: "Não informado")
        addLinha(
            "Categorias",
            if (l.categorias.isEmpty()) "Não informado" else l.categorias.joinToString(", ")
        )
        addLinha("Tipo", l.tipoAcervo.toString())

        return builder
    }

    companion object {
        // Usamos EXTRA_LIVRO para passar APENAS o ID do livro (String)
        const val EXTRA_LIVRO = "EXTRA_LIVRO"
    }

    // HEADER / AÇÕES
    private lateinit var ivBack: ImageView
    private lateinit var ivCapa: ImageView
    private lateinit var ivFavorite: ImageView   // ⭐ ícone de favoritos

    // TEXTO
    private lateinit var tvTitulo: TextView
    private lateinit var tvAutor: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDescricao: TextView

    // BOTÕES
    private lateinit var btnEbook: Button
    private lateinit var btnReserva: Button

    // FIRESTORE / DADOS
    private lateinit var fb: FirebaseFirestore
    private var livroId: String? = null
    private var livro: Livro? = null

    // FAVORITOS
    private val COLLECTION_FAVORITOS = "favoritos"
    private var isFavorite: Boolean = false   // estado atual do favorito

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_details)

        fb = Firebase.firestore
        bindViews()
        setupBack()
        setupReserva()
        setupEbookButton()
        setupFavorite() // ⭐ configurar clique na estrela

        // Agora buscamos só o ID vindo na intent
        livroId = intent.getStringExtra(EXTRA_LIVRO)

        if (livroId == null) {
            Toast.makeText(this, "Livro não encontrado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Desabilita o botão de reserva até carregar o livro
        btnReserva.isEnabled = false

        // Carrega dados do livro
        carregarLivro(livroId!!)

        // Carrega estado inicial de favorito para este user + livro
        carregarStatusFavorito()
    }

    private fun bindViews() {
        ivBack = findViewById(R.id.btnBack)
        ivCapa = findViewById(R.id.ivBookCover)
        ivFavorite = findViewById(R.id.btnFavorite) // ⭐ id do XML da estrela

        tvTitulo = findViewById(R.id.tvBookTitle)
        tvAutor = findViewById(R.id.tvBookAuthor)
        tvInfo = findViewById(R.id.tvBookInfo)
        tvStatus = findViewById(R.id.tvBookStatus)
        tvDescricao = findViewById(R.id.tvBookDescription)

        btnEbook = findViewById(R.id.btnOpenEbook)
        btnReserva = findViewById(R.id.btnSolicitarReserva)
    }

    private fun carregarLivro(id: String) {
        fb.collection("livros")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                val l = doc.toObject(Livro::class.java)
                if (l == null) {
                    Toast.makeText(this, "Livro não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                livro = l
                renderizarLivro(l)
                // só habilita reserva depois de carregar o livro
                btnReserva.isEnabled = (l.statusGeral == StatusLivro.DISPONIVEL)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar livro.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun renderizarLivro(l: Livro) {
        tvTitulo.text = l.titulo
        tvAutor.text = l.autor ?: "Autor não informado"

        tvInfo.text = textoInfoComNegrito(l)

        // Status
        val statusTexto = when (l.statusGeral) {
            StatusLivro.DISPONIVEL -> "Disponível"
            StatusLivro.INDISPONIVEL -> "Indisponível"
            StatusLivro.RESERVADO -> "Reservado"
            StatusLivro.ESGOTADO -> "Esgotado"
            StatusLivro.PENDENTE -> "Pendente"
        }
        tvStatus.text = statusTexto

        // altera a cor do texto conforme o status
        when (l.statusGeral) {
            StatusLivro.DISPONIVEL -> {
                tvStatus.setTextColor(getColor(android.R.color.white))
                tvStatus.setBackgroundColor(getColor(android.R.color.holo_green_dark))
            }
            else -> {
                tvStatus.setTextColor(getColor(android.R.color.white))
                tvStatus.setBackgroundColor(getColor(android.R.color.holo_red_dark))
            }
        }

        // Placeholder por enquanto
        ivCapa.setImageResource(R.drawable.ic_book_placeholder)

        // Descrição
        tvDescricao.text = l.descricao ?: "Descrição não informada"

        // Exibe botão de e-book se existir URL digital ou se for acervo digital
        btnEbook.visibility =
            if (l.tipoAcervo == TipoAcervo.DIGITAL || l.urlAcessoDigital != null) {
                Button.VISIBLE
            } else {
                Button.GONE
            }

        // Reserva habilitada apenas se estiver disponível
        btnReserva.isEnabled = (l.statusGeral == StatusLivro.DISPONIVEL)
        btnReserva.text = if (btnReserva.isEnabled) {
            "Solicitar reserva"
        } else {
            when (l.statusGeral) {
                StatusLivro.DISPONIVEL -> "Solicitar reserva"
                StatusLivro.RESERVADO -> "Já reservado"
                StatusLivro.INDISPONIVEL -> "Indisponível"
                StatusLivro.ESGOTADO -> "Esgotado"
                StatusLivro.PENDENTE -> "Pendente"
            }
        }
    }

    private fun setupBack() {
        ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupReserva() {
        btnReserva.setOnClickListener {
            val userId = SessionManager.obterUsuarioId(this)
            val l = livro ?: return@setOnClickListener

            if (userId == null) {
                Toast.makeText(this, "Faça login para reservar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (l.statusGeral != StatusLivro.DISPONIVEL) {
                Toast.makeText(this, "Livro não está disponível para reserva.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Confirmar reserva")
                .setMessage("Deseja reservar \"${l.titulo}\"?")
                .setPositiveButton("Sim") { _, _ ->
                    criarReserva(userId, l)
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }

    private fun setupEbookButton() {
        btnEbook.setOnClickListener {
            val link = livro?.urlAcessoDigital
            if (link.isNullOrBlank()) {
                Toast.makeText(this, "Nenhum e-book disponível.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data = link.toUri()
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Não foi possível abrir o e-book.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------- RESERVA NO FIRESTORE ----------------

    private fun criarReserva(usuarioId: String, l: Livro) {
        val docRef = fb.collection("reservas").document()

        val reserva = Reserva(
            id = docRef.id,
            usuarioId = usuarioId,
            livroId = l.id,
            dataReserva = Timestamp.now(),
            expiraEm = Timestamp.now().let {
                Timestamp(it.seconds + 7 * 24 * 60 * 60, it.nanoseconds)
            },
            status = StatusReserva.PENDENTE
        )

        // 1️⃣ Criar a reserva
        docRef.set(reserva)
            .addOnSuccessListener {
                // 2️⃣ Atualizar o status do livro para INDISPONIVEL no Firestore
                fb.collection("livros")
                    .document(l.id)
                    .update("statusGeral", StatusLivro.INDISPONIVEL.name)
                    .addOnSuccessListener {
                        // 3️⃣ Atualizar o objeto em memória e re-renderizar a tela
                        livro = livro?.copy(statusGeral = StatusLivro.INDISPONIVEL)
                        livro?.let { livroAtualizado ->
                            renderizarLivro(livroAtualizado)
                        }

                        // Opcional: desabilitar botão e mudar texto
                        btnReserva.isEnabled = false
                        btnReserva.text = "Já reservado"

                        Toast.makeText(this, "Reserva criada!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Reserva criada, mas falhou ao atualizar o status do livro.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao reservar.", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------- FAVORITOS ----------------

    /**
     * Configura o clique na estrela de favoritos.
     * Usa um docId determinístico: userId_livroId
     */
    private fun setupFavorite() {
        // Garante que o botão é clicável
        ivFavorite.isClickable = true

        ivFavorite.setOnClickListener {
            // DEBUG VISÍVEL: só para ter certeza que o clique está chegando aqui
            // (se quiser depois, pode remover esse Toast)
            Toast.makeText(this, "Processando favorito...", Toast.LENGTH_SHORT).show()

            val userId = SessionManager.obterUsuarioId(this)
            val idLivroAtual = livroId
            val l = livro

            if (userId == null) {
                Toast.makeText(this, "Faça login para favoritar livros.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (idLivroAtual.isNullOrBlank() || l == null) {
                Toast.makeText(this, "Livro não carregado ainda.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val favDocId = "${userId}_${idLivroAtual}"

            if (!isFavorite) {
                // Adiciona aos favoritos
                val dadosFavorito = hashMapOf(
                    "id" to favDocId,
                    "usuarioId" to userId,
                    "livroId" to idLivroAtual,
                    "tituloLivro" to l.titulo,
                    "autorLivro" to (l.autor ?: ""),
                    "criadoEm" to Timestamp.now()
                )

                Firebase.firestore.collection(COLLECTION_FAVORITOS)
                    .document(favDocId)
                    .set(dadosFavorito)
                    .addOnSuccessListener {
                        isFavorite = true
                        atualizarIconeFavorito()
                        Toast.makeText(this, "Adicionado aos favoritos!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Erro ao favoritar: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                // Remove dos favoritos
                Firebase.firestore.collection(COLLECTION_FAVORITOS)
                    .document(favDocId)
                    .delete()
                    .addOnSuccessListener {
                        isFavorite = false
                        atualizarIconeFavorito()
                        Toast.makeText(this, "Removido dos favoritos.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Erro ao remover favorito: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    /**
     * Consulta inicial: verifica se este livro já está favoritado pelo usuário logado
     * e ajusta o estado da estrela.
     */
    private fun carregarStatusFavorito() {
        val userId = SessionManager.obterUsuarioId(this)
        val idLivroAtual = livroId

        if (userId == null || idLivroAtual.isNullOrBlank()) {
            // sem usuário logado, mantemos isFavorite = false e ícone "desligado"
            atualizarIconeFavorito()
            return
        }

        val favDocId = "${userId}_${idLivroAtual}"

        fb.collection(COLLECTION_FAVORITOS)
            .document(favDocId)
            .get()
            .addOnSuccessListener { doc ->
                isFavorite = doc.exists()
                atualizarIconeFavorito()
            }
            .addOnFailureListener {
                // Em caso de erro, não quebra a UI; só mantém como não favorito
                isFavorite = false
                atualizarIconeFavorito()
            }
    }

    /**
     * Atualiza o ícone visual de acordo com o estado atual (isFavorite).
     * Aqui assumimos que você tem dois drawables:
     *  - ic_favorite_border  (estrela vazia)
     *  - ic_favorite_filled  (estrela cheia)
     */
    private fun atualizarIconeFavorito() {
        if (!::ivFavorite.isInitialized) return

        // Altera apenas a cor de fundo para indicar favorito ou não
        val bgColor = if (isFavorite) {
            // Cor de destaque quando estiver favoritado
            getColor(android.R.color.holo_orange_light)
        } else {
            // Sem destaque quando não estiver favoritado
            getColor(android.R.color.transparent)
        }
        ivFavorite.setBackgroundColor(bgColor)
    }
}