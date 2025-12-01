package com.example.emabooks

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class ActivityReservaDetails : BaseActivity () {

    companion object {
        const val EXTRA_RESERVA_ID = "EXTRA_RESERVA_ID"
    }

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var ivBookCover: ImageView
    private lateinit var tvBookTitle: TextView
    private lateinit var tvBookAuthor: TextView
    private lateinit var tvBookInfo: TextView
    private lateinit var tvReservaStatus: TextView
    private lateinit var tvReservaInfo: TextView
    private lateinit var tvDescricao: TextView
    private lateinit var btnRenovarReserva: Button
    private lateinit var progressLoadingReserva: ProgressBar

    // Firestore
    private lateinit var fb: FirebaseFirestore

    // Dados
    private var reserva: Reserva? = null
    private var livro: Livro? = null
    private var reservaId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserva_details)

        fb = Firebase.firestore
        bindViews()
        setupBack()

        reservaId = intent.getStringExtra(EXTRA_RESERVA_ID)
        if (reservaId.isNullOrBlank()) {
            Toast.makeText(this, "Reserva não encontrada.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        carregarReserva(reservaId!!)
    }

    // ---------------- BIND & UI ----------------

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBackReserva)
        ivBookCover = findViewById(R.id.ivBookCoverReserva)
        tvBookTitle = findViewById(R.id.tvBookTitleReserva)
        tvBookAuthor = findViewById(R.id.tvBookAuthorReserva)
        tvBookInfo = findViewById(R.id.tvBookInfoReserva)
        tvReservaStatus = findViewById(R.id.tvReservaStatus)
        tvReservaInfo = findViewById(R.id.tvReservaInfo)
        tvDescricao = findViewById(R.id.tvBookDescriptionReserva)
        btnRenovarReserva = findViewById(R.id.btnRenovarReserva)
        progressLoadingReserva = findViewById(R.id.progressLoadingReserva)
    }

    private fun setupBack() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // ---------------- CARREGAMENTO FIRESTORE ----------------

    private fun carregarReserva(id: String) {
        mostrarLoading(true)

        fb.collection("reservas")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                val r = doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                if (r == null) {
                    mostrarLoading(false)
                    Toast.makeText(this, "Reserva não encontrada.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                reserva = r
                carregarLivro(r.livroId)
            }
            .addOnFailureListener {
                mostrarLoading(false)
                Toast.makeText(this, "Erro ao carregar reserva.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarLivro(livroId: String) {
        fb.collection("livros")
            .document(livroId)
            .get()
            .addOnSuccessListener { doc ->
                val l = doc.toObject(Livro::class.java)?.copy(id = doc.id)
                if (l == null) {
                    // Se o livro não for encontrado, mostramos só os dados da reserva
                    livro = null
                    renderizar()
                } else {
                    livro = l
                    renderizar()
                }
            }
            .addOnFailureListener {
                livro = null
                renderizar()
            }
    }

    private fun mostrarLoading(mostrar: Boolean) {
        progressLoadingReserva.visibility = if (mostrar) ProgressBar.VISIBLE else ProgressBar.GONE
    }

    // ---------------- RENDERIZAÇÃO ----------------

    private fun renderizar() {
        val r = reserva
        if (r == null) {
            mostrarLoading(false)
            return
        }

        val l = livro

        // Título / autor – se não achar o livro, mostra só IDs
        if (l != null) {
            tvBookTitle.text = l.titulo
            tvBookAuthor.text = l.autor ?: "Autor não informado"
            tvBookInfo.text = textoInfoLivroComNegrito(l)
            tvDescricao.text = l.descricao ?: "Descrição não informada"
        } else {
            tvBookTitle.text = "Livro não encontrado"
            tvBookAuthor.text = "ID do livro: ${r.livroId}"
            tvBookInfo.text = "Não foi possível carregar os dados do livro."
            tvDescricao.text = ""
        }

        // Capa – placeholder
        ivBookCover.setImageResource(R.drawable.ic_book_placeholder)

        // Status da reserva
        tvReservaStatus.text = "Status: ${r.status}"
        when (r.status) {
            StatusReserva.ATIVA -> {
                tvReservaStatus.setTextColor(getColor(android.R.color.white))
                tvReservaStatus.setBackgroundColor(getColor(android.R.color.holo_green_dark))
            }
            StatusReserva.PENDENTE -> {
                tvReservaStatus.setTextColor(getColor(android.R.color.white))
                tvReservaStatus.setBackgroundColor(getColor(android.R.color.holo_orange_dark))
            }
            StatusReserva.CONVERTIDA -> {
                tvReservaStatus.setTextColor(getColor(android.R.color.white))
                tvReservaStatus.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
            }
            StatusReserva.CANCELADA -> {
                tvReservaStatus.setTextColor(getColor(android.R.color.white))
                tvReservaStatus.setBackgroundColor(getColor(android.R.color.darker_gray))
            }
        }

        // Info da reserva: data, expiração, fila
        tvReservaInfo.text = textoInfoReserva(r)

        // Setup do botão de renovar
        setupRenovarReserva(r)

        mostrarLoading(false)
    }

    private fun textoInfoLivroComNegrito(l: Livro): CharSequence {
        val builder = android.text.SpannableStringBuilder()

        fun addLinha(rotulo: String, valor: String) {
            val start = builder.length
            builder.append("$rotulo: ")
            builder.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start,
                start + rotulo.length + 1,
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

    private fun textoInfoReserva(r: Reserva): CharSequence {
        val builder = android.text.SpannableStringBuilder()

        fun addLinha(rotulo: String, valor: String) {
            val start = builder.length
            builder.append("$rotulo: ")
            builder.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start,
                start + rotulo.length + 1,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(valor)
            builder.append("\n")
        }

        val dataReservaStr = r.dataReserva?.toDate()?.let {
            android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", it).toString()
        } ?: "Não informado"

        val expiraEmStr = r.expiraEm?.toDate()?.let {
            android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", it).toString()
        } ?: "Não informado"

        addLinha("Data da reserva", dataReservaStr)
        addLinha("Expira em", expiraEmStr)
        addLinha("Posição na fila", r.posicaoFila.toString())

        return builder
    }

// ---------------- RENOVAÇÃO DE RESERVA ----------------

    private fun setupRenovarReserva(r: Reserva) {

        // Só permite renovar se a reserva estiver ATIVA
        if (r.status == StatusReserva.ATIVA) {

            btnRenovarReserva.apply {
                isEnabled = true
                text = "Renovar reserva"
            }

            btnRenovarReserva.setOnClickListener {

                AlertDialog.Builder(this)
                    .setTitle("Renovar reserva")
                    .setMessage("Deseja renovar esta reserva por mais 7 dias?")
                    .setPositiveButton("Sim") { _, _ ->
                        renovarReserva(r)
                    }
                    .setNegativeButton("Não", null)
                    .show()
            }

        } else {
            btnRenovarReserva.apply {
                isEnabled = false
                text = "Renovação indisponível"
            }
        }
    }

    private fun renovarReserva(r: Reserva) {
        mostrarLoading(true)

        // Nova data de expiração = agora + 7 dias
        val agora = com.google.firebase.Timestamp.now()
        val novaDataExpiracao = com.google.firebase.Timestamp(
            agora.seconds + (7 * 24 * 60 * 60),
            agora.nanoseconds
        )

        fb.collection("reservas")
            .document(r.id)
            .update("expiraEm", novaDataExpiracao)
            .addOnSuccessListener {
                Toast.makeText(this, "Reserva renovada por mais 7 dias.", Toast.LENGTH_SHORT).show()

                // Atualiza em memória e re-renderiza
                reserva = r.copy(expiraEm = novaDataExpiracao)
                renderizar()
            }
            .addOnFailureListener {
                mostrarLoading(false)
                Toast.makeText(this, "Erro ao renovar reserva.", Toast.LENGTH_SHORT).show()
            }
    }
}