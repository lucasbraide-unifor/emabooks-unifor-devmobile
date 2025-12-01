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
import java.util.Calendar

class ActivityAdminNovaReserva : AppCompatActivity() {

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_RESERVAS = "reservas"
    private val COLLECTION_USERS = "users"
    private val COLLECTION_LIVROS = "livros"

    // Views
    private lateinit var toolbarNovaReserva: MaterialToolbar
    private lateinit var tilUsuarioId: TextInputLayout
    private lateinit var etUsuarioId: TextInputEditText
    private lateinit var tilLivroId: TextInputLayout
    private lateinit var etLivroId: TextInputEditText
    private lateinit var tilDiasEmprestimo: TextInputLayout
    private lateinit var etDiasEmprestimo: TextInputEditText
    private lateinit var tvErroNovaReserva: TextView
    private lateinit var progressNovaReserva: ProgressBar
    private lateinit var btnSalvarNovaReserva: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_nova_reserva)

        fb = Firebase.firestore
        bindViews()
        setupToolbar()
        setupListeners()
    }

    private fun bindViews() {
        toolbarNovaReserva = findViewById(R.id.toolbarNovaReserva)
        tilUsuarioId = findViewById(R.id.tilUsuarioId)
        etUsuarioId = findViewById(R.id.etUsuarioId)
        tilLivroId = findViewById(R.id.tilLivroId)
        etLivroId = findViewById(R.id.etLivroId)
        tilDiasEmprestimo = findViewById(R.id.tilDiasEmprestimo)
        etDiasEmprestimo = findViewById(R.id.etDiasEmprestimo)
        tvErroNovaReserva = findViewById(R.id.tvErroNovaReserva)
        progressNovaReserva = findViewById(R.id.progressNovaReserva)
        btnSalvarNovaReserva = findViewById(R.id.btnSalvarNovaReserva)
    }

    private fun setupToolbar() {
        toolbarNovaReserva.setNavigationOnClickListener {
            finish()
        }
        toolbarNovaReserva.title = "Nova reserva"
    }

    private fun setupListeners() {
        btnSalvarNovaReserva.setOnClickListener {
            salvarNovaReserva()
        }
    }

    private fun mostrarErro(msg: String) {
        tvErroNovaReserva.text = msg
        tvErroNovaReserva.visibility = View.VISIBLE
    }

    private fun limparErros() {
        tvErroNovaReserva.text = ""
        tvErroNovaReserva.visibility = View.GONE
        tilUsuarioId.error = null
        tilLivroId.error = null
        tilDiasEmprestimo.error = null
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressNovaReserva.visibility = View.VISIBLE
            btnSalvarNovaReserva.isEnabled = false
        } else {
            progressNovaReserva.visibility = View.GONE
            btnSalvarNovaReserva.isEnabled = true
        }
    }

    private fun salvarNovaReserva() {
        limparErros()

        val usuarioId = etUsuarioId.text?.toString()?.trim().orEmpty()
        val livroId = etLivroId.text?.toString()?.trim().orEmpty()
        val diasStr = etDiasEmprestimo.text?.toString()?.trim().orEmpty()

        // -------- VALIDAÇÕES BÁSICAS DO FORM --------
        if (usuarioId.isEmpty()) {
            tilUsuarioId.error = "Informe o ID do usuário"
            mostrarErro("Verifique os campos: usuário é obrigatório.")
            return
        }

        if (livroId.isEmpty()) {
            tilLivroId.error = "Informe o ID do livro"
            mostrarErro("Verifique os campos: livro é obrigatório.")
            return
        }

        val dias = diasStr.toIntOrNull()
        if (dias == null) {
            tilDiasEmprestimo.error = "Informe um número de dias válido"
            mostrarErro("Dias de empréstimo inválidos.")
            return
        }

        if (dias <= 0 || dias > 60) {
            tilDiasEmprestimo.error = "Informe um valor entre 1 e 60 dias"
            mostrarErro("Dias de empréstimo devem estar entre 1 e 60.")
            return
        }

        setLoading(true)

        // -------- VALIDAÇÃO NO FIRESTORE: USUÁRIO EXISTE? --------
        fb.collection(COLLECTION_USERS).document(usuarioId).get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    setLoading(false)
                    tilUsuarioId.error = "Usuário não encontrado"
                    mostrarErro("Usuário não encontrado na base.")
                    return@addOnSuccessListener
                }

                // -------- VALIDAÇÃO NO FIRESTORE: LIVRO EXISTE? --------
                fb.collection(COLLECTION_LIVROS).document(livroId).get()
                    .addOnSuccessListener { livroDoc ->
                        if (!livroDoc.exists()) {
                            setLoading(false)
                            tilLivroId.error = "Livro não encontrado"
                            mostrarErro("Livro não encontrado na base.")
                            return@addOnSuccessListener
                        }

                        // Tudo ok, cria a reserva
                        criarReserva(usuarioId, livroId, dias)
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        mostrarErro("Erro ao validar livro: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                mostrarErro("Erro ao validar usuário: ${e.localizedMessage}")
            }
    }

    private fun criarReserva(usuarioId: String, livroId: String, dias: Int) {
        val agora = Calendar.getInstance()

        val dataReserva = Timestamp(agora.time)

        val expiraCalendar = Calendar.getInstance().apply {
            time = agora.time
            add(Calendar.DAY_OF_YEAR, dias)
        }
        val expiraEm = Timestamp(expiraCalendar.time)

        val reservaData = hashMapOf(
            "usuarioId" to usuarioId,
            "livroId" to livroId,
            "dataReserva" to dataReserva,
            "expiraEm" to expiraEm,
            "status" to "PENDENTE" // compatível com StatusReserva.PENDENTE
        )

        fb.collection(COLLECTION_RESERVAS)
            .add(reservaData)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Reserva criada com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                mostrarErro("Erro ao salvar reserva: ${e.localizedMessage}")
            }
    }
}