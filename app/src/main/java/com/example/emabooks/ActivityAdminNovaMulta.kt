package com.example.emabooks

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class ActivityAdminNovaMulta : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_MULTAS = "multas"
    private val COLLECTION_USERS = "users"
    private val COLLECTION_LIVROS = "livros"
    private val COLLECTION_RESERVAS = "reservas"

    // Views
    private lateinit var toolbarNovaMulta: MaterialToolbar
    private lateinit var tilUsuarioIdNovaMulta: TextInputLayout
    private lateinit var etUsuarioIdNovaMulta: TextInputEditText
    private lateinit var tilLivroIdNovaMulta: TextInputLayout
    private lateinit var etLivroIdNovaMulta: TextInputEditText
    private lateinit var tilReservaIdNovaMulta: TextInputLayout
    private lateinit var etReservaIdNovaMulta: TextInputEditText
    private lateinit var tilDescricaoNovaMulta: TextInputLayout
    private lateinit var etDescricaoNovaMulta: TextInputEditText
    private lateinit var tilValorNovaMulta: TextInputLayout
    private lateinit var etValorNovaMulta: TextInputEditText
    private lateinit var tvUsuarioPreviewNovaMulta: TextView
    private lateinit var tvLivroPreviewNovaMulta: TextView
    private lateinit var tvReservaPreviewNovaMulta: TextView
    private lateinit var tvErroNovaMulta: TextView
    private lateinit var progressNovaMulta: ProgressBar
    private lateinit var btnCancelarNovaMulta: MaterialButton
    private lateinit var btnSalvarNovaMulta: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_nova_multa)

        fb = Firebase.firestore
        bindViews()
        setupToolbar()
        setupListeners()
    }

    private fun bindViews() {
        toolbarNovaMulta = findViewById(R.id.toolbarNovaMulta)
        tilUsuarioIdNovaMulta = findViewById(R.id.tilUsuarioIdNovaMulta)
        etUsuarioIdNovaMulta = findViewById(R.id.etUsuarioIdNovaMulta)
        tilLivroIdNovaMulta = findViewById(R.id.tilLivroIdNovaMulta)
        etLivroIdNovaMulta = findViewById(R.id.etLivroIdNovaMulta)
        tilReservaIdNovaMulta = findViewById(R.id.tilReservaIdNovaMulta)
        etReservaIdNovaMulta = findViewById(R.id.etReservaIdNovaMulta)
        tilDescricaoNovaMulta = findViewById(R.id.tilDescricaoNovaMulta)
        etDescricaoNovaMulta = findViewById(R.id.etDescricaoNovaMulta)
        tilValorNovaMulta = findViewById(R.id.tilValorNovaMulta)
        etValorNovaMulta = findViewById(R.id.etValorNovaMulta)
        tvUsuarioPreviewNovaMulta = findViewById(R.id.tvUsuarioPreviewNovaMulta)
        tvLivroPreviewNovaMulta = findViewById(R.id.tvLivroPreviewNovaMulta)
        tvReservaPreviewNovaMulta = findViewById(R.id.tvReservaPreviewNovaMulta)
        tvErroNovaMulta = findViewById(R.id.tvErroNovaMulta)
        progressNovaMulta = findViewById(R.id.progressNovaMulta)
        btnCancelarNovaMulta = findViewById(R.id.btnCancelarNovaMulta)
        btnSalvarNovaMulta = findViewById(R.id.btnSalvarNovaMulta)
    }

    private fun setupToolbar() {
        toolbarNovaMulta.setNavigationOnClickListener {
            finish()
        }
        toolbarNovaMulta.title = "Nova multa"
    }

    private fun setupListeners() {
        btnCancelarNovaMulta.setOnClickListener {
            finish()
        }

        btnSalvarNovaMulta.setOnClickListener {
            salvarNovaMulta()
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            progressNovaMulta.visibility = View.VISIBLE
            btnSalvarNovaMulta.isEnabled = false
            btnCancelarNovaMulta.isEnabled = false
        } else {
            progressNovaMulta.visibility = View.GONE
            btnSalvarNovaMulta.isEnabled = true
            btnCancelarNovaMulta.isEnabled = true
        }
    }

    private fun mostrarErro(msg: String) {
        tvErroNovaMulta.text = msg
        tvErroNovaMulta.visibility = View.VISIBLE
    }

    private fun limparErros() {
        tvErroNovaMulta.text = ""
        tvErroNovaMulta.visibility = View.GONE
        tilUsuarioIdNovaMulta.error = null
        tilLivroIdNovaMulta.error = null
        tilReservaIdNovaMulta.error = null
        tilDescricaoNovaMulta.error = null
        tilValorNovaMulta.error = null
        tvUsuarioPreviewNovaMulta.text = "Usuário: -"
        tvLivroPreviewNovaMulta.text = "Livro: -"
        tvReservaPreviewNovaMulta.text = "Reserva: -"
    }

    private fun salvarNovaMulta() {
        limparErros()

        val matricula = etUsuarioIdNovaMulta.text?.toString()?.trim().orEmpty()
        val livroId = etLivroIdNovaMulta.text?.toString()?.trim().orEmpty()
        val reservaId = etReservaIdNovaMulta.text?.toString()?.trim().orEmpty()
        val descricao = etDescricaoNovaMulta.text?.toString()?.trim().orEmpty()
        val valorStr = etValorNovaMulta.text?.toString()?.trim().orEmpty()

        tvUsuarioPreviewNovaMulta.text = "Usuário: -"
        tvLivroPreviewNovaMulta.text = "Livro: -"
        tvReservaPreviewNovaMulta.text = "Reserva: -"

        // ---- Validações de formulário ----
        if (matricula.isEmpty()) {
            tilUsuarioIdNovaMulta.error = "Informe a matrícula do usuário"
            mostrarErro("Usuário é obrigatório.")
            return
        }

        if (descricao.isEmpty()) {
            tilDescricaoNovaMulta.error = "Informe uma descrição"
            mostrarErro("Descrição da multa é obrigatória.")
            return
        }

        val valor = valorStr.replace(",", ".").toDoubleOrNull()
        if (valor == null || valor <= 0.0) {
            tilValorNovaMulta.error = "Informe um valor válido (> 0)"
            mostrarErro("Valor da multa inválido.")
            return
        }

        setLoading(true)

        // ---- Valida usuário pela matrícula ----
        fb.collection(COLLECTION_USERS)
            .whereEqualTo("matricula", matricula)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    setLoading(false)
                    tilUsuarioIdNovaMulta.error = "Matrícula não encontrada"
                    mostrarErro("Usuário não encontrado na base.")
                    return@addOnSuccessListener
                }

                val userDoc = querySnapshot.documents.first()
                val usuarioId = userDoc.id
                val nomeUsuario = userDoc.getString("nomeCompleto") ?: matricula
                tvUsuarioPreviewNovaMulta.text = "Usuário: $nomeUsuario (Matrícula: $matricula)"

                // Se livro foi informado, valida; senão segue
                if (livroId.isNotEmpty()) {
                    fb.collection(COLLECTION_LIVROS).document(livroId).get()
                        .addOnSuccessListener { livroDoc ->
                            if (!livroDoc.exists()) {
                                setLoading(false)
                                tilLivroIdNovaMulta.error = "Livro não encontrado"
                                mostrarErro("Livro não encontrado na base.")
                                return@addOnSuccessListener
                            }

                            val tituloLivro = livroDoc.getString("titulo") ?: livroId
                            tvLivroPreviewNovaMulta.text = "Livro: $tituloLivro"

                            validarReservaECriarMulta(
                                usuarioId = usuarioId,
                                livroId = livroId,
                                reservaId = reservaId,
                                descricao = descricao,
                                valor = valor
                            )
                        }
                        .addOnFailureListener { e ->
                            setLoading(false)
                            mostrarErro("Erro ao validar livro: ${e.localizedMessage}")
                        }
                } else {
                    validarReservaECriarMulta(
                        usuarioId = usuarioId,
                        livroId = null,
                        reservaId = reservaId,
                        descricao = descricao,
                        valor = valor
                    )
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                mostrarErro("Erro ao validar usuário: ${e.localizedMessage}")
            }
    }

    private fun validarReservaECriarMulta(
        usuarioId: String,
        livroId: String?,
        reservaId: String?,
        descricao: String,
        valor: Double
    ) {
        val reservaTrim = reservaId?.trim().orEmpty()

        if (reservaTrim.isNotEmpty()) {
            fb.collection(COLLECTION_RESERVAS).document(reservaTrim).get()
                .addOnSuccessListener { reservaDoc ->
                    if (!reservaDoc.exists()) {
                        setLoading(false)
                        tilReservaIdNovaMulta.error = "Reserva não encontrada"
                        mostrarErro("Reserva não encontrada na base.")
                        return@addOnSuccessListener
                    }

                    tvReservaPreviewNovaMulta.text = "Reserva: $reservaTrim"

                    criarMulta(usuarioId, livroId, reservaTrim, descricao, valor)
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    mostrarErro("Erro ao validar reserva: ${e.localizedMessage}")
                }
        } else {
            criarMulta(usuarioId, livroId, null, descricao, valor)
        }
    }

    private fun criarMulta(
        usuarioId: String,
        livroId: String?,
        reservaId: String?,
        descricao: String,
        valor: Double
    ) {
        val agora = Timestamp.now()

        val data = hashMapOf<String, Any>(
            "usuarioId" to usuarioId,
            "descricao" to descricao,
            "valor" to valor,
            "dataMulta" to agora,
            "status" to "PENDENTE",
            "emRegularizacao" to false
        )

        if (!livroId.isNullOrEmpty()) {
            data["livroId"] = livroId
        }

        if (!reservaId.isNullOrEmpty()) {
            data["reservaId"] = reservaId
        }

        fb.collection(COLLECTION_MULTAS)
            .add(data)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Multa criada com sucesso.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                mostrarErro("Erro ao criar multa: ${e.localizedMessage}")
            }
    }
}