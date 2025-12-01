package com.example.emabooks

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class ActivityAdminReservaDetalhe : AppCompatActivity() {

    companion object {
        const val EXTRA_RESERVA_ID = "EXTRA_RESERVA_ID"
    }

    // Firestore
    private lateinit var fb: FirebaseFirestore
    private val COLLECTION_RESERVAS = "reservas"
    private val COLLECTION_USUARIOS = "users"
    private val COLLECTION_LIVROS = "livros"

    // Views
    private lateinit var toolbarReservaDetalhe: MaterialToolbar
    private lateinit var etUsuarioReserva: EditText
    private lateinit var etLivroReserva: EditText
    private lateinit var etDataInicioReserva: EditText
    private lateinit var etDataDevolucaoReserva: EditText
    private lateinit var spinnerStatusReserva: Spinner
    private lateinit var tvErroReserva: TextView
    private lateinit var btnAprovarRenovacao: MaterialButton
    private lateinit var btnRejeitarRenovacao: MaterialButton
    private lateinit var layoutBotoesRenovacao: LinearLayout
    private lateinit var btnCancelarReserva: MaterialButton
    private lateinit var btnSalvarReserva: MaterialButton
    private lateinit var btnEncerrarReserva: MaterialButton
    private lateinit var btnExcluirReserva: MaterialButton

    private var reservaId: String? = null
    private var reservaAtual: Reserva? = null
    private var usuarioIdInterno: String? = null
    private var livroIdInterno: String? = null

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_reserva_detalhe)

        fb = Firebase.firestore
        initViews()
        setupToolbar()
        setupStatusSpinner()
        setupListeners()

        reservaId = intent.getStringExtra(EXTRA_RESERVA_ID)

        if (reservaId != null) {
            carregarReserva(reservaId!!)
        } else {
            // Cadastro novo: esconder botões de encerrar/excluir/renovação
            layoutBotoesRenovacao.visibility = View.GONE
            btnEncerrarReserva.visibility = View.GONE
            btnExcluirReserva.visibility = View.GONE
        }
    }

    private fun initViews() {
        toolbarReservaDetalhe = findViewById(R.id.toolbarReservaDetalhe)
        etUsuarioReserva = findViewById(R.id.etUsuarioReserva)
        etLivroReserva = findViewById(R.id.etLivroReserva)
        etDataInicioReserva = findViewById(R.id.etDataInicioReserva)
        etDataDevolucaoReserva = findViewById(R.id.etDataDevolucaoReserva)
        spinnerStatusReserva = findViewById(R.id.spinnerStatusReserva)
        tvErroReserva = findViewById(R.id.tvErroReserva)
        btnAprovarRenovacao = findViewById(R.id.btnAprovarRenovacao)
        btnRejeitarRenovacao = findViewById(R.id.btnRejeitarRenovacao)
        layoutBotoesRenovacao = findViewById(R.id.layoutBotoesRenovacao)
        btnCancelarReserva = findViewById(R.id.btnCancelarReserva)
        btnSalvarReserva = findViewById(R.id.btnSalvarReserva)
        btnEncerrarReserva = findViewById(R.id.btnEncerrarReserva)
        btnExcluirReserva = findViewById(R.id.btnExcluirReserva)
    }

    private fun setupToolbar() {
        toolbarReservaDetalhe.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupStatusSpinner() {
        val statusValues = StatusReserva.values().map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statusValues
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatusReserva.adapter = adapter
    }

    private fun setupListeners() {
        btnCancelarReserva.setOnClickListener { finish() }

        btnSalvarReserva.setOnClickListener {
            salvarReserva()
        }

        btnEncerrarReserva.setOnClickListener {
            encerrarReserva()
        }

        btnExcluirReserva.setOnClickListener {
            excluirReserva()
        }

        btnAprovarRenovacao.setOnClickListener {
            aprovarRenovacao()
        }

        btnRejeitarRenovacao.setOnClickListener {
            rejeitarRenovacao()
        }
    }

    private fun carregarReserva(id: String) {
        fb.collection(COLLECTION_RESERVAS)
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                val reserva = doc.toObject(Reserva::class.java)
                if (reserva == null) {
                    Toast.makeText(this, "Reserva não encontrada.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                reservaAtual = reserva
                preencherCampos(reserva)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar reserva.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun preencherCampos(reserva: Reserva) {
        // Guarda os IDs reais e depois carrega os nomes para exibir
        usuarioIdInterno = reserva.usuarioId
        livroIdInterno = reserva.livroId

        carregarNomesUsuarioELivro()

        reserva.dataReserva?.toDate()?.let {
            etDataInicioReserva.setText(sdf.format(it))
        }

        reserva.expiraEm?.toDate()?.let {
            etDataDevolucaoReserva.setText(sdf.format(it))
        }

        val index = StatusReserva.values().indexOf(reserva.status)
        if (index >= 0) {
            spinnerStatusReserva.setSelection(index)
        }

        // Renovação ainda não implementada: esconde os botões
        layoutBotoesRenovacao.visibility = View.GONE
        btnAprovarRenovacao.visibility = View.GONE
        btnRejeitarRenovacao.visibility = View.GONE
    }

    /**
     * Carrega o nome do usuário e o título do livro a partir dos IDs salvos em
     * [usuarioIdInterno] e [livroIdInterno] e exibe nos campos de detalhe.
     */
    private fun carregarNomesUsuarioELivro() {
        val usuarioId = usuarioIdInterno
        val livroId = livroIdInterno

        // Buscar nome do usuário
        if (usuarioId != null) {
            fb.collection(COLLECTION_USUARIOS)
                .document(usuarioId)
                .get()
                .addOnSuccessListener { usuarioDoc ->
                    val nomeUsuario = usuarioDoc.getString("nomeCompleto") ?: usuarioId
                    etUsuarioReserva.setText(nomeUsuario)
                }
                .addOnFailureListener {
                    // Em caso de erro, mantém o ID para não ficar vazio
                    etUsuarioReserva.setText(usuarioId)
                }
        }

        // Buscar título do livro
        if (livroId != null) {
            fb.collection(COLLECTION_LIVROS)
                .document(livroId)
                .get()
                .addOnSuccessListener { livroDoc ->
                    val tituloLivro = livroDoc.getString("titulo") ?: livroId
                    etLivroReserva.setText(tituloLivro)
                }
                .addOnFailureListener {
                    etLivroReserva.setText(livroId)
                }
        }
    }

    private fun salvarReserva() {
        tvErroReserva.visibility = View.GONE

        val usuarioCampo = etUsuarioReserva.text.toString().trim()
        val livroCampo = etLivroReserva.text.toString().trim()

        // Se já temos os IDs reais (caso de edição), usamos eles; caso contrário usamos o que foi digitado
        val usuario = usuarioIdInterno ?: usuarioCampo
        val livro = livroIdInterno ?: livroCampo

        val dataInicioStr = etDataInicioReserva.text.toString().trim()
        val dataDevolucaoStr = etDataDevolucaoReserva.text.toString().trim()
        val statusSelecionado = StatusReserva.valueOf(
            spinnerStatusReserva.selectedItem.toString()
        )

        if (usuarioCampo.isEmpty() || livroCampo.isEmpty() || dataInicioStr.isEmpty() || dataDevolucaoStr.isEmpty()) {
            mostrarErro("Preencha todos os campos obrigatórios.")
            return
        }

        val dataInicio: Date
        val dataDevolucao: Date
        try {
            dataInicio = sdf.parse(dataInicioStr)!!
            dataDevolucao = sdf.parse(dataDevolucaoStr)!!
        } catch (e: Exception) {
            mostrarErro("Datas inválidas. Use o formato dd/MM/yyyy.")
            return
        }

        if (dataDevolucao.before(dataInicio)) {
            mostrarErro("Data de devolução não pode ser anterior à data de início.")
            return
        }

        // Validações de negócio básicas (Livro disponível, Usuário ativo)
        validarLivroEUsuarioAntesDeSalvar(usuario, livro) { valido, mensagemErro ->
            if (!valido) {
                mostrarErro(mensagemErro ?: "Não foi possível validar usuário/livro.")
                return@validarLivroEUsuarioAntesDeSalvar
            }

            val dataReservaTs = Timestamp(dataInicio)
            val dataDevolucaoTs = Timestamp(dataDevolucao)

            if (reservaId == null) {
                // Cadastrar novo empréstimo (reserva)
                val docRef = fb.collection(COLLECTION_RESERVAS).document()
                val novaReserva = Reserva(
                    id = docRef.id,
                    usuarioId = usuario,
                    livroId = livro,
                    dataReserva = dataReservaTs,
                    expiraEm = dataDevolucaoTs,
                    status = statusSelecionado
                )

                docRef.set(novaReserva)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Empréstimo cadastrado.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        mostrarErro("Erro ao salvar empréstimo.")
                    }
            } else {
                // Editar empréstimo existente
                val updates = hashMapOf<String, Any>(
                    "dataReserva" to dataReservaTs,
                    "expiraEm" to dataDevolucaoTs,
                    "status" to statusSelecionado
                )

                fb.collection(COLLECTION_RESERVAS)
                    .document(reservaId!!)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Empréstimo atualizado.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        mostrarErro("Erro ao atualizar empréstimo.")
                    }
            }
        }
    }

    /**
     * Validações:
     * - Livro não estiver Disponível -> bloquear
     * - Usuário estiver Inativo -> bloquear
     */
    private fun validarLivroEUsuarioAntesDeSalvar(
        usuarioIdOuNome: String,
        livroIdOuTitulo: String,
        callback: (Boolean, String?) -> Unit
    ) {
        // Aqui estou assumindo que você usa o ID puro do documento.
        // Se estiver usando nome/título, ajuste para query por campo.
        val usuarioRef = fb.collection(COLLECTION_USUARIOS).document(usuarioIdOuNome)
        val livroRef = fb.collection(COLLECTION_LIVROS).document(livroIdOuTitulo)

        usuarioRef.get().continueWithTask { usuarioTask ->
            if (!usuarioTask.isSuccessful || !usuarioTask.result.exists()) {
                throw Exception("Usuário não encontrado.")
            }
            val statusUsuario = usuarioTask.result.getString("status") ?: "ATIVO"
            if (statusUsuario.uppercase() == "INATIVO") {
                throw Exception("Usuário está inativo.")
            }

            livroRef.get()
        }.addOnSuccessListener { livroDoc ->
            if (!livroDoc.exists()) {
                callback(false, "Livro não encontrado.")
                return@addOnSuccessListener
            }

            val statusLivro = livroDoc.getString("status") ?: "DISPONIVEL"
            if (statusLivro.uppercase() != "DISPONIVEL") {
                callback(false, "Livro não está disponível para empréstimo.")
                return@addOnSuccessListener
            }

            callback(true, null)
        }.addOnFailureListener { e ->
            callback(false, e.message)
        }
    }

    private fun aprovarRenovacao() {
        // Placeholder simples: renovação ainda não implementada no modelo atual
        AlertDialog.Builder(this)
            .setTitle("Renovação")
            .setMessage("A funcionalidade de renovação ainda não está disponível nesta versão.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun rejeitarRenovacao() {
        // Placeholder simples: renovação ainda não implementada no modelo atual
        AlertDialog.Builder(this)
            .setTitle("Renovação")
            .setMessage("A funcionalidade de renovação ainda não está disponível nesta versão.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun encerrarReserva() {
        val reserva = reservaAtual ?: return

        AlertDialog.Builder(this)
            .setTitle("Encerrar empréstimo")
            .setMessage("Confirmar encerramento (devolvido)?")
            .setPositiveButton("Sim") { _, _ ->
                // Aqui você pode ajustar para um status existente no seu enum.
                // Por enquanto, apenas mantemos o status atual e registramos a ação.
                val updates = hashMapOf<String, Any>(
                    "status" to reserva.status,
                    "encerradoEm" to Timestamp.now()
                )

                fb.collection(COLLECTION_RESERVAS)
                    .document(reserva.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Empréstimo encerrado (status mantido).", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        mostrarErro("Erro ao encerrar empréstimo.")
                    }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun excluirReserva() {
        val reserva = reservaAtual ?: return

        AlertDialog.Builder(this)
            .setTitle("Excluir empréstimo")
            .setMessage("Tem certeza que deseja excluir este registro?")
            .setPositiveButton("Sim") { _, _ ->
                fb.collection(COLLECTION_RESERVAS)
                    .document(reserva.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Empréstimo excluído.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        mostrarErro("Erro ao excluir empréstimo.")
                    }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun mostrarErro(msg: String) {
        tvErroReserva.text = msg
        tvErroReserva.visibility = View.VISIBLE
    }
}