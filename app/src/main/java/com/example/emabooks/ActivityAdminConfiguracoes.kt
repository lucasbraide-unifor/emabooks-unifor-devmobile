package com.example.emabooks

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText


class ActivityAdminConfiguracoes : AppCompatActivity() {

    // Prefs
    private lateinit var prefs: SharedPreferences

    private val PREFS_NAME = "admin_config_prefs"
    private val KEY_PRAZO_PADRAO_DIAS = "prazo_padrao_dias"
    private val KEY_MAX_RENOVACOES = "max_renovacoes"
    private val KEY_DIAS_AVISO = "dias_aviso"
    private val KEY_ACESS_ALTO_CONTRASTE = "acess_alto_contraste"
    private val KEY_ACESS_FONTE_MAIOR = "acess_fonte_maior"
    private val KEY_ACESS_REDUZIR_ANIM = "acess_reduzir_anim"

    // Views
    private lateinit var toolbarAdminConfig: MaterialToolbar
    private lateinit var tvUsuarioLogadoInfo: TextView

    private lateinit var switchAltoContrasteAdmin: SwitchMaterial
    private lateinit var switchFonteMaiorAdmin: SwitchMaterial
    private lateinit var switchReduzirAnimacoesAdmin: SwitchMaterial

    private lateinit var etPrazoPadraoDias: TextInputEditText
    private lateinit var etMaxRenovacoes: TextInputEditText
    private lateinit var etDiasAvisoDevolucao: TextInputEditText

    private lateinit var tvResumoConfiguracoes: TextView

    private lateinit var btnCancelarConfigAdmin: MaterialButton
    private lateinit var btnSalvarConfigAdmin: MaterialButton

    private lateinit var tvVersaoSistema: TextView
    private lateinit var tvUltimaAtualizacao: TextView
    private lateinit var tvTipoLicenca: TextView

    private lateinit var btnLogoutAdminConfig: android.widget.ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_configuracoes)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        bindViews()
        setupToolbar()
        setupLogout()
        carregarUsuarioLogado()
        carregarConfiguracoes()
        configurarRodape()
        setupListeners()
    }

    private fun bindViews() {
        toolbarAdminConfig = findViewById(R.id.toolbarAdminConfig)
        tvUsuarioLogadoInfo = findViewById(R.id.tvUsuarioLogadoInfo)

        switchAltoContrasteAdmin = findViewById(R.id.switchAltoContrasteAdmin)
        switchFonteMaiorAdmin = findViewById(R.id.switchFonteMaiorAdmin)
        switchReduzirAnimacoesAdmin = findViewById(R.id.switchReduzirAnimacoesAdmin)

        etPrazoPadraoDias = findViewById(R.id.etPrazoPadraoDias)
        etMaxRenovacoes = findViewById(R.id.etMaxRenovacoes)
        etDiasAvisoDevolucao = findViewById(R.id.etDiasAvisoDevolucao)

        tvResumoConfiguracoes = findViewById(R.id.tvResumoConfiguracoes)

        btnCancelarConfigAdmin = findViewById(R.id.btnCancelarConfigAdmin)
        btnSalvarConfigAdmin = findViewById(R.id.btnSalvarConfigAdmin)

        tvVersaoSistema = findViewById(R.id.tvVersaoSistema)
        tvUltimaAtualizacao = findViewById(R.id.tvUltimaAtualizacao)
        tvTipoLicenca = findViewById(R.id.tvTipoLicenca)

        btnLogoutAdminConfig = findViewById(R.id.btnLogoutAdminConfig)
    }

    private fun setupToolbar() {
        toolbarAdminConfig.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupLogout() {
        btnLogoutAdminConfig.setOnClickListener {
            // Simples: volta para tela de login e limpa a pilha
            val intent = Intent(this, ActivityLogin::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finishAffinity()
        }
    }

    private fun carregarUsuarioLogado() {
        val userId = SessionManager.obterUsuarioId(this)
        tvUsuarioLogadoInfo.text = if (userId != null) {
            "ID: $userId"
        } else {
            "Nenhum usuário logado"
        }
    }

    private fun carregarConfiguracoes() {
        val prazoPadrao = prefs.getInt(KEY_PRAZO_PADRAO_DIAS, 7)
        val maxRenov = prefs.getInt(KEY_MAX_RENOVACOES, 2)
        val diasAviso = prefs.getInt(KEY_DIAS_AVISO, 3)

        val altoContraste = prefs.getBoolean(KEY_ACESS_ALTO_CONTRASTE, false)
        val fonteMaior = prefs.getBoolean(KEY_ACESS_FONTE_MAIOR, false)
        val reduzirAnim = prefs.getBoolean(KEY_ACESS_REDUZIR_ANIM, false)

        etPrazoPadraoDias.setText(prazoPadrao.toString())
        etMaxRenovacoes.setText(maxRenov.toString())
        etDiasAvisoDevolucao.setText(diasAviso.toString())

        switchAltoContrasteAdmin.isChecked = altoContraste
        switchFonteMaiorAdmin.isChecked = fonteMaior
        switchReduzirAnimacoesAdmin.isChecked = reduzirAnim

        atualizarResumo()
    }

    private fun configurarRodape() {
        // Obtém a versão do app pelo PackageManager para evitar problemas de BuildConfig
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = pInfo.versionName ?: "1.0"
        tvVersaoSistema.text = "Versão: $versionName"
        // Data e hora atual
        tvUltimaAtualizacao.text = "Última atualização: 01/12/2025"
        tvTipoLicenca.text = "Licença: Acadêmica"
    }

    private fun setupListeners() {
        btnCancelarConfigAdmin.setOnClickListener {
            finish()
        }

        btnSalvarConfigAdmin.setOnClickListener {
            salvarConfiguracoes()
        }

        // Se quiser, poderia chamar atualizarResumo() quando mudar switches,
        // mas por enquanto atualizamos só ao carregar/salvar.
    }

    private fun salvarConfiguracoes() {
        val prazoStr = etPrazoPadraoDias.text?.toString()?.trim().orEmpty()
        val maxRenovStr = etMaxRenovacoes.text?.toString()?.trim().orEmpty()
        val diasAvisoStr = etDiasAvisoDevolucao.text?.toString()?.trim().orEmpty()

        if (prazoStr.isEmpty() || maxRenovStr.isEmpty() || diasAvisoStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        val prazo: Int
        val maxRenov: Int
        val diasAviso: Int

        try {
            prazo = prazoStr.toInt()
            maxRenov = maxRenovStr.toInt()
            diasAviso = diasAvisoStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Insira apenas números válidos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (prazo <= 0 || maxRenov < 0 || diasAviso < 0) {
            Toast.makeText(this, "Valores inválidos. Verifique as configurações.", Toast.LENGTH_SHORT).show()
            return
        }

        val altoContraste = switchAltoContrasteAdmin.isChecked
        val fonteMaior = switchFonteMaiorAdmin.isChecked
        val reduzirAnim = switchReduzirAnimacoesAdmin.isChecked

        prefs.edit()
            .putInt(KEY_PRAZO_PADRAO_DIAS, prazo)
            .putInt(KEY_MAX_RENOVACOES, maxRenov)
            .putInt(KEY_DIAS_AVISO, diasAviso)
            .putBoolean(KEY_ACESS_ALTO_CONTRASTE, altoContraste)
            .putBoolean(KEY_ACESS_FONTE_MAIOR, fonteMaior)
            .putBoolean(KEY_ACESS_REDUZIR_ANIM, reduzirAnim)
            .apply()

        atualizarResumo()

        Toast.makeText(this, "Configurações salvas com sucesso.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun atualizarResumo() {
        val prazo = etPrazoPadraoDias.text?.toString()?.trim().ifNullOrBlank { "—" }
        val maxRenov = etMaxRenovacoes.text?.toString()?.trim().ifNullOrBlank { "—" }
        val diasAviso = etDiasAvisoDevolucao.text?.toString()?.trim().ifNullOrBlank { "—" }

        tvResumoConfiguracoes.text =
            "Prazo padrão: $prazo dias • Máx. renovações: $maxRenov • Aviso: $diasAviso dias antes"
    }

    // Extensão simples para evitar null/blank chato
    private fun String?.ifNullOrBlank(default: () -> String): String {
        return if (this.isNullOrBlank()) default() else this
    }
}