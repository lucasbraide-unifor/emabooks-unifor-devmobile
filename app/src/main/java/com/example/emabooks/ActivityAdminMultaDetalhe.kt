package com.example.emabooks

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ActivityAdminMultaDetalhe : AppCompatActivity() {

    companion object {
        const val EXTRA_MULTA_ID = "EXTRA_MULTA_ID"
    }

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_MULTAS = "multas"
    private val COLLECTION_USERS = "users"
    private val COLLECTION_LIVROS = "livros"
    private val COLLECTION_RESERVAS = "reservas"

    // Views
    private lateinit var toolbarMultaDetalhe: MaterialToolbar
    private lateinit var tilUsuarioIdMulta: TextInputLayout
    private lateinit var etUsuarioIdMulta: TextInputEditText
    private lateinit var tilLivroIdMulta: TextInputLayout
    private lateinit var etLivroIdMulta: TextInputEditText
    private lateinit var tilReservaIdMulta: TextInputLayout
    private lateinit var etReservaIdMulta: TextInputEditText
    private lateinit var tilDescricaoMulta: TextInputLayout
    private lateinit var etDescricaoMulta: TextInputEditText
    private lateinit var tilValorMulta: TextInputLayout
    private lateinit var etValorMulta: TextInputEditText
    private lateinit var tilStatusMulta: TextInputLayout
    private lateinit var actStatusMulta: MaterialAutoCompleteTextView
    private lateinit var switchEmRegularizacao: SwitchMaterial
    private lateinit var tvDataMultaDetalhe: TextView
    private lateinit var tvErroMultaDetalhe: TextView
    private lateinit var progressMultaDetalhe: ProgressBar
    private lateinit var btnExcluirMulta: MaterialButton
    private lateinit var btnCancelarMulta: MaterialButton
    private lateinit var btnSalvarMulta: MaterialButton

    private var multaId: String? = null
    private var multaAtual: Multa? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_multa_detalhe)

        fb = Firebase.firestore
        multaId = intent.getStringExtra(EXTRA_MULTA_ID)

        if (multaId.isNullOrBlank()) {
            Toast.makeText(this, "Multa inválida.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupToolbar()
        setupStatusDropdown()
        setupButtons()
        carregarMulta()
    }

    private fun bindViews() {
        toolbarMultaDetalhe = findViewById(R.id.toolbarMultaDetalhe)
        tilUsuarioIdMulta = findViewById(R.id.tilUsuarioIdMulta)
        etUsuarioIdMulta = findViewById(R.id.etUsuarioIdMulta)
        tilLivroIdMulta = findViewById(R.id.tilLivroIdMulta)
        etLivroIdMulta = findViewById(R.id.etLivroIdMulta)
        tilReservaIdMulta = findViewById(R.id.tilReservaIdMulta)
        etReservaIdMulta = findViewById(R.id.etReservaIdMulta)
        tilDescricaoMulta = findViewById(R.id.tilDescricaoMulta)
        etDescricaoMulta = findViewById(R.id.etDescricaoMulta)
        tilValorMulta = findViewById(R.id.tilValorMulta)
        etValorMulta = findViewById(R.id.etValorMulta)
        tilStatusMulta = findViewById(R.id.tilStatusMulta)
        actStatusMulta = findViewById(R.id.actStatusMulta)
        switchEmRegularizacao = findViewById(R.id.switchEmRegularizacao)
        tvDataMultaDetalhe = findViewById(R.id.tvDataMultaDetalhe)
        tvErroMultaDetalhe = findViewById(R.id.tvErroMultaDetalhe)
        progressMultaDetalhe = findViewById(R.id.progressMultaDetalhe)
        btnExcluirMulta = findViewById(R.id.btnExcluirMulta)
        btnCancelarMulta = findViewById(R.id.btnCancelarMulta)
        btnSalvarMulta = findViewById(R.id.btnSalvarMulta)
    }

    private fun setupToolbar() {
        toolbarMultaDetalhe.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupStatusDropdown() {
        val statusOptions = listOf("Pendente", "Paga")
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            statusOptions
        )
        actStatusMulta.setAdapter(adapter)
    }

    private fun setupButtons() {
        btnCancelarMulta.setOnClickListener {
            finish()
        }

        btnSalvarMulta.setOnClickListener {
            salvarAlteracoes()
        }

        btnExcluirMulta.setOnClickListener {
            confirmarExclusao()
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            progressMultaDetalhe.visibility = View.VISIBLE
            btnSalvarMulta.isEnabled = false
            btnExcluirMulta.isEnabled = false
        } else {
            progressMultaDetalhe.visibility = View.GONE
            btnSalvarMulta.isEnabled = true
            btnExcluirMulta.isEnabled = true
        }
    }

    private fun mostrarErro(msg: String) {
        tvErroMultaDetalhe.text = msg
        tvErroMultaDetalhe.visibility = View.VISIBLE
    }

    private fun limparErros() {
        tvErroMultaDetalhe.text = ""
        tvErroMultaDetalhe.visibility = View.GONE
        tilUsuarioIdMulta.error = null
        tilLivroIdMulta.error = null
        tilReservaIdMulta.error = null
        tilDescricaoMulta.error = null
        tilValorMulta.error = null
        tilStatusMulta.error = null
    }

    private fun carregarMulta() {
        setLoading(true)
        fb.collection(COLLECTION_MULTAS)
            .document(multaId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    setLoading(false)
                    Toast.makeText(this, "Multa não encontrada.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val multa = doc.toObject(Multa::class.java)
                if (multa == null) {
                    setLoading(false)
                    Toast.makeText(this, "Erro ao carregar dados da multa.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                multa.id = doc.id
                multaAtual = multa
                preencherCampos(multa)
                setLoading(false)
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Erro ao carregar multa.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun preencherCampos(multa: Multa) {
        etUsuarioIdMulta.setText(multa.usuarioId)
        etLivroIdMulta.setText(multa.livroId ?: "")
        etReservaIdMulta.setText(multa.reservaId ?: "")
        etDescricaoMulta.setText(multa.descricao)

        if (multa.valor != 0.0) {
            etValorMulta.setText(multa.valor.toString())
        } else {
            etValorMulta.setText("")
        }

        val statusLabel = if (multa.status.equals("PAGA", ignoreCase = true)) {
            "Paga"
        } else {
            "Pendente"
        }
        actStatusMulta.setText(statusLabel, false)

        switchEmRegularizacao.isChecked = multa.emRegularizacao == true

        val data = multa.dataMulta?.toDate()
        val dataTexto = data?.let { dateFormat.format(it) } ?: "-"
        tvDataMultaDetalhe.text = "Data da multa: $dataTexto"
    }

    private fun salvarAlteracoes() {
        limparErros()

        val usuarioId = etUsuarioIdMulta.text?.toString()?.trim().orEmpty()
        val livroId = etLivroIdMulta.text?.toString()?.trim().orEmpty()
        val reservaId = etReservaIdMulta.text?.toString()?.trim().orEmpty()
        val descricao = etDescricaoMulta.text?.toString()?.trim().orEmpty()
        val valorStr = etValorMulta.text?.toString()?.trim().orEmpty()
        val statusLabel = actStatusMulta.text?.toString()?.trim().orEmpty()
        val emRegularizacao = switchEmRegularizacao.isChecked

        // ---- Validações básicas ----
        if (usuarioId.isEmpty()) {
            tilUsuarioIdMulta.error = "Informe o ID do usuário"
            mostrarErro("Verifique o ID do usuário.")
            return
        }

        if (descricao.isEmpty()) {
            tilDescricaoMulta.error = "Informe uma descrição"
            mostrarErro("Descrição da multa é obrigatória.")
            return
        }

        val valor = valorStr.replace(",", ".").toDoubleOrNull()
        if (valor == null || valor <= 0.0) {
            tilValorMulta.error = "Informe um valor válido (> 0)"
            mostrarErro("Valor da multa inválido.")
            return
        }

        if (statusLabel.isEmpty()) {
            tilStatusMulta.error = "Selecione um status"
            mostrarErro("Selecione o status da multa.")
            return
        }

        val status = when (statusLabel.lowercase(Locale.getDefault())) {
            "paga" -> "PAGA"
            else -> "PENDENTE"
        }

        setLoading(true)

        // Valida usuário e livro (e opcionalmente reserva)
        fb.collection(COLLECTION_USERS).document(usuarioId).get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    setLoading(false)
                    tilUsuarioIdMulta.error = "Usuário não encontrado"
                    mostrarErro("Usuário não encontrado na base.")
                    return@addOnSuccessListener
                }

                // Se livroId for informado, valida; senão, segue
                if (livroId.isNotEmpty()) {
                    fb.collection(COLLECTION_LIVROS).document(livroId).get()
                        .addOnSuccessListener { livroDoc ->
                            if (!livroDoc.exists()) {
                                setLoading(false)
                                tilLivroIdMulta.error = "Livro não encontrado"
                                mostrarErro("Livro não encontrado na base.")
                                return@addOnSuccessListener
                            }

                            validarReservaEAtualizar(usuarioId, livroId, reservaId, descricao, valor, status, emRegularizacao)
                        }
                        .addOnFailureListener { e ->
                            setLoading(false)
                            mostrarErro("Erro ao validar livro: ${e.localizedMessage}")
                        }
                } else {
                    validarReservaEAtualizar(usuarioId, null, reservaId, descricao, valor, status, emRegularizacao)
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                mostrarErro("Erro ao validar usuário: ${e.localizedMessage}")
            }
    }

    private fun validarReservaEAtualizar(
        usuarioId: String,
        livroId: String?,
        reservaId: String?,
        descricao: String,
        valor: Double,
        status: String,
        emRegularizacao: Boolean
    ) {
        val reservaTrim = reservaId?.trim().orEmpty()

        if (reservaTrim.isNotEmpty()) {
            fb.collection(COLLECTION_RESERVAS).document(reservaTrim).get()
                .addOnSuccessListener { reservaDoc ->
                    if (!reservaDoc.exists()) {
                        setLoading(false)
                        tilReservaIdMulta.error = "Reserva não encontrada"
                        mostrarErro("Reserva não encontrada na base.")
                        return@addOnSuccessListener
                    }
                    atualizarMulta(usuarioId, livroId, reservaTrim, descricao, valor, status, emRegularizacao)
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    mostrarErro("Erro ao validar reserva: ${e.localizedMessage}")
                }
        } else {
            atualizarMulta(usuarioId, livroId, null, descricao, valor, status, emRegularizacao)
        }
    }

    private fun atualizarMulta(
        usuarioId: String,
        livroId: String?,
        reservaId: String?,
        descricao: String,
        valor: Double,
        status: String,
        emRegularizacao: Boolean
    ) {
        val updates = mutableMapOf<String, Any>(
            "usuarioId" to usuarioId,
            "descricao" to descricao,
            "valor" to valor,
            "status" to status,
            "emRegularizacao" to emRegularizacao
        )

        if (livroId != null) {
            updates["livroId"] = livroId
        } else {
            updates["livroId"] = ""
        }

        if (reservaId != null) {
            updates["reservaId"] = reservaId
        } else {
            updates["reservaId"] = ""
        }

        fb.collection(COLLECTION_MULTAS)
            .document(multaId!!)
            .update(updates as Map<String, Any>)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Multa atualizada com sucesso.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                mostrarErro("Erro ao atualizar multa: ${e.localizedMessage}")
            }
    }

    private fun confirmarExclusao() {
        val multa = multaAtual
        if (multa == null) {
            Toast.makeText(this, "Multa inválida.", Toast.LENGTH_SHORT).show()
            return
        }

        if (multa.emRegularizacao == true) {
            Toast.makeText(
                this,
                "Não é possível remover multa em processo de regularização.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir multa")
            .setMessage("Tem certeza que deseja excluir esta multa?")
            .setPositiveButton("Excluir") { _, _ ->
                excluirMulta()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirMulta() {
        setLoading(true)
        fb.collection(COLLECTION_MULTAS)
            .document(multaId!!)
            .delete()
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Multa excluída.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                mostrarErro("Erro ao excluir multa: ${e.localizedMessage}")
            }
    }
}