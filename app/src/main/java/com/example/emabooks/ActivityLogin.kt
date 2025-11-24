package com.example.emabooks

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ActivityLogin : AppCompatActivity() {

    private lateinit var fb: FirebaseFirestore

    // Views
    private lateinit var etEmail: EditText
    private lateinit var etSenha: EditText
    private lateinit var tvErroLogin: TextView
    private lateinit var btnEntrar: Button
    private lateinit var tvCadastro: TextView
    private lateinit var tvEsqueciSenha: TextView

    private fun seedLivrosExemplo() {
        val fb = FirebaseFirestore.getInstance()
        val colecao = fb.collection("livros")

        val livros = listOf(
            mapOf(
                "id" to "livro001",
                "titulo" to "Arquitetura de Sistemas Distribuídos",
                "autor" to "Andrew Tanenbaum",
                "anoPublicacao" to 2020,
                "editora" to "Pearson",
                "tipoAcervo" to "FISICO",
                "categorias" to listOf("Computação", "Redes", "Sistemas"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/8231856-L.jpg",
                "statusGeral" to "DISPONIVEL",
                "totalExemplaresFisicos" to 5,
                "exemplaresFisicosDisponiveis" to 3,
                "urlAcessoDigital" to null,
                "descricao" to "Introdução prática e teórica aos principais conceitos de sistemas distribuídos modernos.",
                "createdAt" to null,
                "updatedAt" to null
            ),
            mapOf(
                "id" to "livro002",
                "titulo" to "Clean Code",
                "autor" to "Robert C. Martin",
                "anoPublicacao" to 2008,
                "editora" to "Prentice Hall",
                "tipoAcervo" to "DIGITAL",
                "categorias" to listOf("Programação"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/5185341-L.jpg",
                "statusGeral" to "DISPONIVEL",
                "totalExemplaresFisicos" to 0,
                "exemplaresFisicosDisponiveis" to 0,
                "urlAcessoDigital" to "https://filesamples.com/samples/document/pdf/sample3.pdf",
                "descricao" to "Livro clássico sobre boas práticas de código limpo, legível e fácil de manter.",
                "createdAt" to null,
                "updatedAt" to null
            ),
            mapOf(
                "id" to "livro003",
                "titulo" to "Inteligência Artificial Moderna",
                "autor" to "Stuart Russell",
                "anoPublicacao" to 2021,
                "editora" to "Elsevier",
                "tipoAcervo" to "FISICO",
                "categorias" to listOf("IA", "Computação"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/10523386-L.jpg",
                "statusGeral" to "INDISPONIVEL",
                "totalExemplaresFisicos" to 2,
                "exemplaresFisicosDisponiveis" to 0,
                "urlAcessoDigital" to null,
                "descricao" to "Visão abrangente e atualizada dos fundamentos e aplicações de inteligência artificial.",
                "createdAt" to null,
                "updatedAt" to null
            ),

            // ---- MAIS 7 LIVROS ----

            mapOf(
                "id" to "livro004",
                "titulo" to "Estruturas de Dados e Algoritmos em Java",
                "autor" to "Michael T. Goodrich",
                "anoPublicacao" to 2016,
                "editora" to "Wiley",
                "tipoAcervo" to "FISICO",
                "categorias" to listOf("Programação"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/240727-L.jpg",
                "statusGeral" to "DISPONIVEL",
                "totalExemplaresFisicos" to 4,
                "exemplaresFisicosDisponiveis" to 2,
                "urlAcessoDigital" to null,
                "descricao" to "Abordagem didática de estruturas de dados e algoritmos utilizando Java como linguagem base.",
                "createdAt" to null,
                "updatedAt" to null
            ),
            mapOf(
                "id" to "livro005",
                "titulo" to "Padrões de Projeto",
                "autor" to "Erich Gamma",
                "anoPublicacao" to 1994,
                "editora" to "Addison-Wesley",
                "tipoAcervo" to "DIGITAL",
                "categorias" to listOf("Arquitetura de Software"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/8235086-L.jpg",
                "statusGeral" to "DISPONIVEL",
                "totalExemplaresFisicos" to 0,
                "exemplaresFisicosDisponiveis" to 0,
                "urlAcessoDigital" to "https://filesamples.com/samples/document/pdf/sample1.pdf",
                "descricao" to "Referência clássica sobre padrões de projeto orientados a objetos para projetos reutilizáveis.",
                "createdAt" to null,
                "updatedAt" to null
            ),
            mapOf(
                "id" to "livro006",
                "titulo" to "Design de Interfaces",
                "autor" to "Alan Cooper",
                "anoPublicacao" to 2014,
                "editora" to "Wiley",
                "tipoAcervo" to "FISICO",
                "categorias" to listOf("UX/UI"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/8319252-L.jpg",
                "statusGeral" to "DISPONIVEL",
                "totalExemplaresFisicos" to 3,
                "exemplaresFisicosDisponiveis" to 1,
                "urlAcessoDigital" to null,
                "descricao" to "Guia prático de design de interfaces focado em experiência do usuário e usabilidade.",
                "createdAt" to null,
                "updatedAt" to null
            ),
            mapOf(
                "id" to "livro007",
                "titulo" to "Deep Learning",
                "autor" to "Ian Goodfellow",
                "anoPublicacao" to 2016,
                "editora" to "MIT Press",
                "tipoAcervo" to "DIGITAL",
                "categorias" to listOf("IA", "Machine Learning"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/8313781-L.jpg",
                "statusGeral" to "DISPONIVEL",
                "totalExemplaresFisicos" to 0,
                "exemplaresFisicosDisponiveis" to 0,
                "urlAcessoDigital" to "https://filesamples.com/samples/document/pdf/sample2.pdf",
                "descricao" to "Livro avançado sobre deep learning, cobrindo teoria, modelos e aplicações práticas.",
                "createdAt" to null,
                "updatedAt" to null
            ),
            mapOf(
                "id" to "livro008",
                "titulo" to "Introdução ao Machine Learning",
                "autor" to "Ethem Alpaydin",
                "anoPublicacao" to 2020,
                "editora" to "MIT Press",
                "tipoAcervo" to "FISICO",
                "categorias" to listOf("IA"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/10909225-L.jpg",
                "statusGeral" to "INDISPONIVEL",
                "totalExemplaresFisicos" to 1,
                "exemplaresFisicosDisponiveis" to 0,
                "urlAcessoDigital" to null,
                "descricao" to "Introdução acessível aos principais algoritmos e conceitos de aprendizado de máquina.",
                "createdAt" to null,
                "updatedAt" to null
            ),
            mapOf(
                "id" to "livro009",
                "titulo" to "Sistemas Operacionais Modernos",
                "autor" to "Andrew Tanenbaum",
                "anoPublicacao" to 2015,
                "editora" to "Pearson",
                "tipoAcervo" to "FISICO",
                "categorias" to listOf("Computação"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/7984913-L.jpg",
                "statusGeral" to "DISPONIVEL",
                "totalExemplaresFisicos" to 6,
                "exemplaresFisicosDisponiveis" to 3,
                "urlAcessoDigital" to null,
                "descricao" to "Estudo completo sobre conceitos, arquitetura e implementação de sistemas operacionais.",
                "createdAt" to null,
                "updatedAt" to null
            ),
            mapOf(
                "id" to "livro010",
                "titulo" to "O Programador Pragmático",
                "autor" to "Andrew Hunt",
                "anoPublicacao" to 2019,
                "editora" to "Addison-Wesley",
                "tipoAcervo" to "DIGITAL",
                "categorias" to listOf("Carreira", "Programação"),
                "capaUrl" to "https://covers.openlibrary.org/b/id/240727-L.jpg",
                "statusGeral" to "DISPONIVEL",
                "totalExemplaresFisicos" to 0,
                "exemplaresFisicosDisponiveis" to 0,
                "urlAcessoDigital" to "https://filesamples.com/samples/document/pdf/sample4.pdf",
                "descricao" to "Conselhos práticos para desenvolvimento de software com foco em qualidade e evolução de carreira.",
                "createdAt" to null,
                "updatedAt" to null
            )
        )

        livros.forEach { livro ->
            val idDoc = livro["id"] as String
            colecao.document(idDoc).set(livro)
                .addOnSuccessListener {
                    // opcional: log ou Toast
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }
    }

    private val collectionName = "user" // mesma coleção usada no cadastro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // seedLivrosExemplo()

        fb = Firebase.firestore

        // Bind
        etEmail = findViewById(R.id.etEmail)
        etSenha = findViewById(R.id.etSenha)
        tvErroLogin = findViewById(R.id.tvErroLogin)
        btnEntrar = findViewById(R.id.btnEntrar)
        tvCadastro = findViewById(R.id.tvCadastro)
        tvEsqueciSenha = findViewById(R.id.tvEsqueciSenha)

        // Navegação para cadastro
        tvCadastro.setOnClickListener {
            startActivity(Intent(this, ActivityRegister::class.java))
        }

        // Navegação para "esqueci minha senha"
        tvEsqueciSenha.setOnClickListener {
            startActivity(Intent(this, ActivityForgotPassword::class.java))
            Toast.makeText(this, "Fluxo de recuperação em construção", Toast.LENGTH_SHORT).show()
        }

        // Login
        btnEntrar.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        showError(null) // limpa mensagem

        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString()

        // Validações (requisitos)
        if (email.isEmpty() || senha.isEmpty()) {
            showError("Preencha todos os campos")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || senha.length < 6) {
            showError("Insira um e-mail e senha válidos")
            return
        }

        setLoading(true)

        // Busca usuário por e-mail
        fb.collection(collectionName)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    setLoading(false)
                    showError("E-mail ou senha incorretos")
                    return@addOnSuccessListener
                }

                val doc = snap.documents.first()
                val senhaSalva = doc.getString("senha_demo") ?: ""

                if (senha == senhaSalva) {
                    setLoading(false)
                    Toast.makeText(this, "Login bem-sucedido", Toast.LENGTH_SHORT).show()

                    val userId = doc.id             // ID do documento na coleção "user"
                    SessionManager.salvarUsuarioId(this, userId)

                    // Se logar correto -> Redireciona pra Home
                    startActivity(Intent(this, ActivityHome::class.java))
                    finish()

                } else {
                    setLoading(false)
                    showError("E-mail ou senha incorretos")
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                // Mostra a causa real para facilitar debug (ex.: PERMISSION_DENIED)
                showError("Falha na autenticação: ${e.message}")
            }
    }

    private fun showError(msg: String?) {
        if (msg.isNullOrBlank()) {
            tvErroLogin.visibility = View.GONE
        } else {
            tvErroLogin.text = msg
            tvErroLogin.visibility = View.VISIBLE
        }
    }

    private fun setLoading(loading: Boolean) {
        btnEntrar.isEnabled = !loading
        tvCadastro.isEnabled = !loading
        tvEsqueciSenha.isEnabled = !loading
        btnEntrar.text = if (loading) "Entrando..." else "Entrar"
    }
}